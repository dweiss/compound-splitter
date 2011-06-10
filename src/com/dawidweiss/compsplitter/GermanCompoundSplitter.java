package com.dawidweiss.compsplitter;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.UnicodeUtil;
import org.apache.lucene.util.fst.*;
import org.apache.lucene.util.fst.FST.Arc;
import org.apache.lucene.util.fst.FST.INPUT_TYPE;

import com.dawidweiss.compsplitter.tools.InputStreamDataInput;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * Compound splitter for German. 
 * http://german.about.com/library/verbs/blverb_pre01.htm
 * http://www.canoo.net/services/WordformationRules/Komposition/N-Comp/Adj+N/Komp+N.html?MenuId=WordFormation115012
 */
public class GermanCompoundSplitter
{
    private final static FST<Object> morphy;
    private final static FST<Object> morphemes;

    static
    {
        try
        {
            morphy = readMorphyFST();
            morphemes = createMorphemesFST();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public GermanCompoundSplitter()
    {
    }

    public static void main(String [] args) throws IOException
    {
        GermanCompoundSplitter splitter = new GermanCompoundSplitter();
        String [] inputs =
        {
            "schweinerei", // not: schwein:er:ei
            "anwendungsprogrammschnittstelle", // "anwendung.s.programm.schnittstelle",
            "bewegungsachse", // "bewegung.s.achse",
            "vergewaltigungsopfer", // vergewaltig.ung.opfer

            "Flüchtlings-lager",
            "Militär-offensive",
            "Kosten-explosion",
            "Privat-haushalt",
            "Infektions-quelle",
            "Projekt-management",
            "Lern-effekt", // missing word in the dictionary (?)
            "Monats-bericht",
            "Entwicklungs-projekt",
            "Unternehmens-kultur",
            "Bevölkerungs-wachstum",
            "Reichstags-abgeordneter",
            "Städte-partnerschaft",
            "Auftrags-eingang",

            "zeitschriften-redaktionen",
            "Gebrauchtwagen",
            "Hochbahn",
            "Gebrauchtwagen",
            "Höher-versicherung",

            "Sünder-ecke",

            // these seeme to be lexicalized in morphy
            "Abteil-ende",
            "Außen-stürmer",
            "Beileids-karte",
            "Comic-strip",
            "Eichen-seide",
            "Flügel-stürmer",
            
            "versandabteilungen",
            "abteilungsleiterposition",
            "Ausstrahlungsnotizen",
            
            "Stadt-plan-ersatz",
            "Konsumentenbeschimpfung",
            "puffer-bau-stein",
            "gelbrot",
        };

        for (String s : inputs)
        {
            System.out.println("#" + s);
            s = s.toLowerCase().replace("-", "");
            splitter.split(s);
            System.out.println("------");
        }
    }

    private static enum ChunkType
    {
        GLUE_MORPHEME, WORD,
    }

    private static class Chunk
    {
        int start;
        int end;

        ChunkType type;
        FST.Arc<Object> arc;

        public Chunk(int start, int end, FST.Arc<Object> arc)
        {
            this.start = start;
            this.end = end;
            this.type = ChunkType.WORD;
            this.arc = arc;
        }

        public Chunk(int offset, int end)
        {
            this.start = offset;
            this.end = end;
            this.type = ChunkType.GLUE_MORPHEME;
        }

        public CharSequence toString(IntsRef codePoints)
        {
            StringBuilder b = new StringBuilder(UnicodeUtil.newString(codePoints.ints,
                start, end - start)).reverse();
            switch (type)
            {
                case WORD:
                    b.append("<" + getPosTag(arc) + ">");
                    break;
                case GLUE_MORPHEME:
                    b.append("<G>");
            }
            return b.toString();
        }
    }

    private static class DecompositionListener
    {
        private IntsRef codePoints;

        public DecompositionListener(IntsRef utf32)
        {
            this.codePoints = utf32;
        }

        public void decomposition(ArrayDeque<Chunk> chunks)
        {
            Iterator<Chunk> i = chunks.descendingIterator();
            while (i.hasNext())
            {
                Chunk ch = i.next();
                System.out.print(ch.toString(codePoints));

                if (i.hasNext()) System.out.print(".");
            }
            System.out.println("");
        }
    }

    private int [] maxPaths;

    private String [] split(String word) throws IOException
    {
        final IntsRef utf32 = UTF16ToUTF32(word, new IntsRef(0));
        reverse(utf32);

        final ArrayDeque<Chunk> stack = new ArrayDeque<Chunk>();
        final DecompositionListener dl = new DecompositionListener(utf32);

        maxPaths = new int [utf32.length + 1];
        Arrays.fill(maxPaths, Integer.MAX_VALUE);
        matchWord(stack, dl, utf32, utf32.offset);

        return null;
    }

    private static interface PathVisitor
    {
        public boolean visit(IntsRef path);
    }

    public static CharSequence getPosTag(Arc<Object> arc)
    {
        final List<String> completions = new ArrayList<String>();
        IntsRef path = new IntsRef(0);
        visitFinalStatesFrom(path, morphy, arc, new PathVisitor()
        {
            public boolean visit(IntsRef path)
            {
                String full = UnicodeUtil.newString(path.ints, 0, path.length);
                String tag = full.substring(full.indexOf('\t') + 1);
                //completions.add(tag);
                return true;
            }
        });

        return Joiner.on("|").join(completions);
    }

    private static <T> void visitFinalStatesFrom(IntsRef path, FST<T> fst, Arc<T> arc,
        PathVisitor pathVisitor)
    {
        try
        {
            FST.Arc<T> scratch = new FST.Arc<T>();
            fst.readFirstTargetArc(arc, arc);
            while (true)
            {
                if (arc.label == FST.END_LABEL) {
                    pathVisitor.visit(path);
                } else {
                    final int save = path.length;

                    path.grow(path.length + 1);
                    path.ints[save] = arc.label;
                    path.length++;
                    visitFinalStatesFrom(path, fst, scratch.copyFrom(arc), pathVisitor);
                    path.length = save;
                }

                if (arc.isLast()) 
                    break;
                fst.readNextArc(arc);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void matchWord(ArrayDeque<Chunk> stack, DecompositionListener dl,
        IntsRef utf32, int offset) throws IOException
    {
        FST.Arc<Object> arc = morphy.getFirstArc(new FST.Arc<Object>());
        FST.Arc<Object> scratch = new FST.Arc<Object>();
        List<Chunk> wordsFromHere = Lists.newArrayList();

        for (int i = offset; i < utf32.length; i++)
        {
            int chr = utf32.ints[i];

            arc = morphy.findTargetArc(chr, arc, arc);
            if (arc == null) break;

            if (morphy.findTargetArc('\t', arc, scratch) != null)
            {
                Chunk ch = new Chunk(offset, i + 1, new Arc<Object>().copyFrom(scratch));
                wordsFromHere.add(ch);
            }
        }

        for (int j = wordsFromHere.size(); --j >= 0;)
        {
            Chunk ch = wordsFromHere.get(j);

            if (stack.size() + 1 > maxPaths[ch.end])
                continue;
            maxPaths[ch.end] = stack.size() + 1;

            stack.addLast(ch);
            if (ch.end == utf32.offset + utf32.length)
            {
                dl.decomposition(stack);
            }
            else
            {
                // no glue.
                matchWord(stack, dl, utf32, ch.end);
                // with glue.
                matchGlueMorpheme(stack, dl, utf32, ch.end);
            }
            stack.removeLast();
        }
    }

    private void matchGlueMorpheme(ArrayDeque<Chunk> stack, DecompositionListener dl,
        IntsRef utf32, final int offset) throws IOException
    {
        FST.Arc<Object> arc = morphemes.getFirstArc(new FST.Arc<Object>());

        for (int i = offset; i < utf32.length; i++)
        {
            int chr = utf32.ints[i];

            arc = morphemes.findTargetArc(chr, arc, arc);
            if (arc == null) break;

            if (arc.isFinal())
            {
                Chunk ch = new Chunk(offset, i + 1);
                stack.addLast(ch);
                if (i + 1 < utf32.offset + utf32.length)
                {
                    matchWord(stack, dl, utf32, i + 1);
                }
                stack.removeLast();
            }
        }
    }

    private void reverse(IntsRef utf32)
    {
        int l = 0, r = utf32.length - 1;
        while (l < r)
        {
            int tmp = utf32.ints[l];
            utf32.ints[l] = utf32.ints[r];
            utf32.ints[r] = tmp;
            l++;
            r--;
        }
    }

    private IntsRef UTF16ToUTF32(CharSequence s, IntsRef scratchIntsRef)
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
     * Load morphy FST.
     */
    private static FST<Object> readMorphyFST()
    {
        try
        {
            final InputStream is = GermanCompoundSplitter.class
                .getResourceAsStream("dict-inverted.fst");
            final FST<Object> fst = new FST<Object>(new InputStreamDataInput(is),
                NoOutputs.getSingleton());
            is.close();
            return fst;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static FST<Object> createMorphemesFST() throws IOException
    {
        String [] morphemes =
        {
            "e", "es", "en", "er", "n", "ens", "ns", "s"
        };

        // Inverse and sort.
        for (int i = 0; i < morphemes.length; i++)
        {
            morphemes[i] = new StringBuilder(morphemes[i]).reverse().toString();
        }
        Arrays.sort(morphemes);

        final Builder<Object> builder = new Builder<Object>(INPUT_TYPE.BYTE4,
            NoOutputs.getSingleton());
        final Object nothing = NoOutputs.getSingleton().getNoOutput();
        for (String morpheme : morphemes)
        {
            builder.add(morpheme, nothing);
        }
        return builder.finish();
    }
}
