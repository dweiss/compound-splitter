package org.apache.lucene.analysis.de.compounds;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.*;

import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.UnicodeUtil;
import org.apache.lucene.util.fst.*;
import org.apache.lucene.util.fst.FST.INPUT_TYPE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple greedy compound splitter for German. Objects of this class are <b>not thread
 * safe</b> and must not be used concurrently.
 */
public class GermanCompoundSplitter
{
    /*
     * Ideas for improvement: Strip off affixes?
     * http://german.about.com/library/verbs/blverb_pre01.htm Use POS tags and
     * morphological patterns, as described here? This will probably be difficult without
     * a disambiguation engine in place, otherwise lots of things will match.
     * http://www.canoo
     * .net/services/WordformationRules/Komposition/N-Comp/Adj+N/Komp+N.html
     * ?MenuId=WordFormation115012
     */

    private static Logger log = LoggerFactory
            .getLogger(GermanCompoundSplitter.class);
    /**
     * A static FSA with inflected and base surface forms from Morphy.
     * 
     * @see "http://www.wolfganglezius.de/doku.php?id=cl:surfaceForms"
     */
    private static FST<Object> surfaceForms;

    /**
     * A static FSA with glue glueMorphemes. This could be merged into a single FSA
     * together with {@link #surfaceForms}, but I leave it separate for now.
     */
    private static FST<Object> glueMorphemes;

    /**
     * left-to-right word encoding symbol (FST).
     */
    static final char LTR_SYMBOL = '>';

    /**
     * right-to-left word encoding symbol (FST).
     */
    static final char RTL_SYMBOL = '<';



    /**
     * Category for a given chunk of a compound.
     */
    public static enum ChunkType
    {
        GLUE_MORPHEME, WORD,
    }

    /**
     * A slice of a compound word.
     */
    public final class Chunk
    {
        public final int start;
        public final int end;
        public final ChunkType type;

        Chunk(int start, int end, ChunkType type)
        {
            this.start = start;
            this.end = end;
            this.type = type;
        }

        @Override
        public String toString()
        {
            final StringBuilder b = new StringBuilder(
                UnicodeUtil.newString(utf32.ints, start, end - start)).reverse();

            if (type == ChunkType.GLUE_MORPHEME) 
                b.append("<G>");

            return b.toString();
        }
    }

    /**
     * A decomposition listener accepts potential decompositions of a word.
     */
    public static interface DecompositionListener
    {
        /**
         * @param utf32 Full unicode points of the input sequence.
         * @param chunks Chunks with decomposed parts and matching regions.
         */
        void decomposition(IntsRef utf32, ArrayDeque<Chunk> chunks);
    }

    /**
     * Full unicode points representation of the input compound.
     */
    private IntsRef utf32 = new IntsRef(0);

    /**
     * This array stores the minimum number of decomposition words during traversals to
     * avoid splitting a larger word into smaller chunks.
     */
    private IntsRef maxPaths = new IntsRef(0);

    /**
     * Reusable array of decomposition chunks.
     */
    private final ArrayDeque<Chunk> chunks = new ArrayDeque<Chunk>();

    /**
     * A decomposition listener accepts potential decompositions of a word.
     */
    private DecompositionListener listener;

    /**
     * String builder for the result of {@link #split(CharSequence)}.
     */
    private final StringBuilder builder = new StringBuilder();


    /**
     * Load and initialize static data structures.
     */
    public static void initFSTs(String fstFile)
    {
        try
        {
            surfaceForms = readMorphyFST(fstFile);
            glueMorphemes = createMorphemesFST();
        }
        catch (IOException e)
        {
            log.error(e.getMessage()
                    + "    Failed to initialize static data structures for German compound splitter.");
        }
    }


    /**
     * Splits the input sequence of characters into separate words if this sequence is
     * potentially a compound word.
     * 
     * @param word The word to be split.
     * @return Returns <code>null</code> if this word is not recognized at all. Returns a
     *         character sequence with '.'-delimited compound chunks (if ambiguous
     *         interpretations are possible, they are separated by a ',' character). The
     *         returned buffer will change with each call to <code>split</code> so copy the
     *         content if needed.
     */
    public CharSequence split(CharSequence word)
    {
        try
        {
            // Lowercase and reverse the input.
            this.builder.setLength(0);
            this.builder.append(word);
            this.builder.reverse();
            for (int i = builder.length(); --i > 0;)
                builder.setCharAt(i, Character.toLowerCase(builder.charAt(i)));
            this.utf32 = UTF16ToUTF32(builder, utf32);
            builder.setLength(0);

            this.listener = new DecompositionListener()
            {
                public void decomposition(IntsRef utf32, ArrayDeque<Chunk> chunks)
                {
                    if (builder.length() > 0)
                        builder.append(",");

                    boolean first = true;
                    Iterator<Chunk> i = chunks.descendingIterator();
                    while (i.hasNext())
                    {
                        Chunk chunk = i.next();
                        if (chunk.type == ChunkType.WORD)
                        {
                            if (!first) builder.append('.');
                            first = false;
                            builder.append(chunk.toString());
                        }
                    }
                }
            };

            maxPaths.grow(utf32.length + 1);
            Arrays.fill(maxPaths.ints, 0, utf32.length + 1, Integer.MAX_VALUE);
            matchWord(utf32, utf32.offset);

            return builder.length() == 0 ? null : builder;
        }
        catch (IOException e)
        {
            // Shouldn't happen, but just in case.
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * Consume a word, then recurse into glue morphemes/ further words.
     */
    private void matchWord(IntsRef utf32, int offset) throws IOException
    {
        FST.Arc<Object> arc = surfaceForms.getFirstArc(new FST.Arc<Object>());
        FST.Arc<Object> scratch = new FST.Arc<Object>();
        List<Chunk> wordsFromHere = new ArrayList<Chunk>();

        for (int i = offset; i < utf32.length; i++)
        {
            int chr = utf32.ints[i];

            arc = surfaceForms.findTargetArc(chr, arc, arc,
                    surfaceForms.getBytesReader());
            if (arc == null) break;

            if (surfaceForms.findTargetArc(RTL_SYMBOL, arc, scratch,
                    surfaceForms.getBytesReader()) != null)
            {
                Chunk ch = new Chunk(offset, i + 1, ChunkType.WORD);
                wordsFromHere.add(ch);
            }
        }

        int [] maxPaths = this.maxPaths.ints;
        for (int j = wordsFromHere.size(); --j >= 0;)
        {
            final Chunk ch = wordsFromHere.get(j);

            if (chunks.size() + 1 > maxPaths[ch.end]) continue;
            maxPaths[ch.end] = chunks.size() + 1;

            chunks.addLast(ch);
            if (ch.end == utf32.offset + utf32.length)
            {
                listener.decomposition(this.utf32, chunks);
            }
            else
            {
                // no glue.
                matchWord(utf32, ch.end);
                // with glue.
                matchGlueMorpheme(utf32, ch.end);
            }
            chunks.removeLast();
        }
    }

    /**
     * Consume a maximal glue morpheme, if any, and consume the next word.
     */
    private void matchGlueMorpheme(IntsRef utf32, final int offset) throws IOException
    {
        FST.Arc<Object> arc = glueMorphemes.getFirstArc(new FST.Arc<Object>());

        for (int i = offset; i < utf32.length; i++)
        {
            int chr = utf32.ints[i];

            arc = glueMorphemes.findTargetArc(chr, arc, arc,
                    glueMorphemes.getBytesReader());
            if (arc == null) break;

            if (arc.isFinal())
            {
                Chunk ch = new Chunk(offset, i + 1, ChunkType.GLUE_MORPHEME);
                chunks.addLast(ch);
                if (i + 1 < utf32.offset + utf32.length)
                {
                    matchWord(utf32, i + 1);
                }
                chunks.removeLast();
            }
        }
    }

    /**
     * Convert a character sequence <code>s</code> into full unicode codepoints.
     */
    private static IntsRef UTF16ToUTF32(CharSequence s, IntsRef scratchIntsRef)
    {
        int charIdx = 0;
        int intIdx = 0;
        final int charLimit = s.length();
        while (charIdx < charLimit)
        {
            scratchIntsRef.grow(intIdx + 1);
            final int utf32 = Character.codePointAt(s, charIdx);
            scratchIntsRef.ints[intIdx] = utf32;
            charIdx += Character.charCount(utf32);
            intIdx++;
        }
        scratchIntsRef.length = intIdx;
        return scratchIntsRef;
    }

    /**
     * Load surface forms FST.
     */
    private static FST<Object> readMorphyFST(String fstFile) throws IOException
    {
        final InputStream is = new FileInputStream(fstFile);
        final FST<Object> fst = new FST<Object>(new InputStreamDataInput(is),
                NoOutputs.getSingleton());
        is.close();
        return fst;
    }

    /**
     * Create glue morphemes FST.
     */
    private static FST<Object> createMorphemesFST() throws IOException
    {
        String[] morphemes =
        {
            "e", "es", "en", "er", "n", "ens", "ns", "s"
        };

        // Inverse and sort.
        for (int i = 0; i < morphemes.length; i++)
        {
            morphemes[i] = new StringBuilder(morphemes[i]).reverse().toString();
        }
        Arrays.sort(morphemes);

        // Build FST.
        final Builder<Object> builder = new Builder<Object>(INPUT_TYPE.BYTE4,
            NoOutputs.getSingleton());
        final Object nothing = NoOutputs.getSingleton().getNoOutput();

        for (String morpheme : morphemes)
        {
            int[] morphemeCodePoints = new int[morpheme.length()];
            for (int i = 0; i < morpheme.length(); i++)
            {
                morphemeCodePoints[i] = morpheme.codePointAt(i);
            }
            builder.add(new IntsRef(morphemeCodePoints, 0, morpheme.length()),
                    nothing);
        }
        return builder.finish();
    }
}

