package com.dawidweiss.compsplitter;

import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.NoOutputs;

import com.dawidweiss.compsplitter.tools.InputStreamDataInput;

/**
 * Compound splitter for German.
 * http://german.about.com/library/verbs/blverb_pre01.htm
 */
public class GermanCompoundSplitter
{
    private final static FST<Object> morphy;

    static
    {
        morphy = readMorphyFST();
    }

    public GermanCompoundSplitter()
    {
        
    }
    
    public static void main(String [] args) throws IOException
    {
        GermanCompoundSplitter splitter = new GermanCompoundSplitter();
        String [] inputs = {
            "schweinerei", // not: schwein:er:ei
            "anwendungsprogrammschnittstelle", // "anwendung.s.programm.schnittstelle",
            "bewegungsachse", //"bewegung.s.achse",
            "vergewaltigungsopfer", // vergewaltig.ung.opfer
        };
        for (String s : inputs) {
            System.out.println(s);
            System.out.println(splitter.split(s));
            System.out.println("------");
        }
    }

    private String[] split(String word) throws IOException
    {
        IntsRef utf32 = UTF16ToUTF32(word, new IntsRef(0));
        reverse(utf32);

        // descend into the automaton and match words.
        FST.Arc<Object> arc = morphy.getFirstArc(new FST.Arc<Object>());
        FST.Arc<Object> scratch = new FST.Arc<Object>(); 

        for (int i = utf32.offset; i < utf32.offset + utf32.length; i++)
        {
            int chr = utf32.ints[i];

            arc = morphy.findTargetArc(chr, arc, arc);
            if (arc == null) 
                break;
            
            System.out.println(chr + " " + (char) chr);
            if (morphy.findTargetArc('\t', arc, scratch) != null) {
                System.out.println("F");                
            }
        }

        return null;
    }

    private void reverse(IntsRef utf32)
    {
        int l = 0, r = utf32.length - 1;
        while (l < r) {
            int tmp = utf32.ints[l];
            utf32.ints[l] = utf32.ints[r];
            utf32.ints[r] = tmp;
            l++; r--;
        }
    }

    private IntsRef UTF16ToUTF32(CharSequence s, IntsRef scratchIntsRef)
    {
        int charIdx = 0;
        int intIdx = 0;
        final int charLimit = s.length();
        while (charIdx < charLimit) {
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
            final InputStream is = GermanCompoundSplitter.class.getResourceAsStream("dict-inverted.fst");
            final FST<Object> fst = new FST<Object>(new InputStreamDataInput(is), NoOutputs.getSingleton());
            is.close();
            return fst;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
