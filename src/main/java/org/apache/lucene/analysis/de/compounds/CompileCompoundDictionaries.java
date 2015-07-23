package org.apache.lucene.analysis.de.compounds;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.UnicodeUtil;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.FST.INPUT_TYPE;
import org.apache.lucene.util.fst.NoOutputs;


/**
 * Compile an FSA from an UTF-8 text file (must be properly sorted).
 */
public class CompileCompoundDictionaries
{
    public static void main(String [] args) throws Exception
    {
        if (args.length < 1)
        {
            System.out.println("Args: input1.txt input2.txt ...");
            System.exit(-1);
        }

        final HashSet<BytesRef> words = new HashSet<BytesRef>();
        for (int i = 0; i < args.length; i++)
        {
            int count = 0;
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(args[i]), "UTF-8"));

            Pattern pattern = Pattern.compile("\\s+");
            String line, last = null;
            StringBuilder buffer = new StringBuilder();
            while ((line = reader.readLine()) != null)
            {
                if (line.indexOf('#') >= 0)
                    continue;

                line = pattern.split(line)[0].trim();
                line = line.toLowerCase();

                if (line.equals(last)) continue;
                last = line;

                /*
                 * Add the word to the hash set in left-to-right characters order and reversed
                 * for easier matching later on.
                 */

                buffer.setLength(0);
                buffer.append(line);
                final int len = buffer.length();

                buffer.append(GermanCompoundSplitter.LTR_SYMBOL);
                words.add(new BytesRef(buffer));

                buffer.setLength(len);
                buffer.reverse().append(GermanCompoundSplitter.RTL_SYMBOL);
                words.add(new BytesRef(buffer));
                if ((++count % 100000) == 0) System.err.println("Line: " + count);
            }
            reader.close();

            System.out.println(String.format("%s, words: %d", args[i], count));
        }

        final BytesRef [] all = new BytesRef [words.size()];
        words.toArray(all);

        Arrays.sort(all, BytesRef.getUTF8SortedAsUnicodeComparator());
        serialize("src/main/resources/words.fst", all);
    }

    private static void serialize(String file, BytesRef [] all) throws IOException
    {
        final Object nothing = NoOutputs.getSingleton().getNoOutput();
        final Builder<Object> builder = new Builder<Object>(INPUT_TYPE.BYTE4,
            NoOutputs.getSingleton());
        final IntsRef intsRef = new IntsRef(0);
        for (BytesRef br : all)
        {
            GermanCompoundSplitter.UTF16ToUTF32(new String(br.bytes, "UTF-8"), intsRef);
            builder.add(intsRef, nothing);
        }
        final FST<Object> fst = builder.finish();

        final OutputStreamDataOutput out = new OutputStreamDataOutput(
            new FileOutputStream(file));
        fst.save(out);
        out.close();
    }
}
