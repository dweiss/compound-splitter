package com.dawidweiss.compsplitter.tools;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;

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
public class CompileFSA
{
    public static void main(String [] args) throws Exception
    {
        if (args.length != 1)
        {
            System.out.println("Args: input.txt");
            System.exit(-1);
        }

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(args[0]), "UTF-8"));

        final HashSet<BytesRef> words = new HashSet<BytesRef>();
        String line, last = null;
        int count = 0;
        while ((line = reader.readLine()) != null) {
            line = line.split("\\s+")[0].trim();
            if (line.equals(last))
                continue;
            last = line;
            words.add(new BytesRef(line));
            if ((++count % 100000) == 0)
                System.err.println("Line: " + count);
        }
        System.out.println("Words: " + count);

        final BytesRef [] all = new BytesRef [words.size()];
        words.toArray(all);
        Arrays.sort(all, BytesRef.getUTF8SortedAsUnicodeComparator());

        serialize("src/main/resources/words.fst", all);
    }

    private static void serialize(String file, BytesRef [] all) throws IOException
    {
        final Object nothing = NoOutputs.getSingleton().getNoOutput();
        final Builder<Object> builder = new Builder<Object>(INPUT_TYPE.BYTE4, NoOutputs.getSingleton());
        final IntsRef intsRef = new IntsRef(0);
        for (BytesRef br : all) {
            UnicodeUtil.UTF8toUTF32(br, intsRef);
            builder.add(intsRef, nothing);
        }
        final FST<Object> fst = builder.finish();

        final OutputStreamDataOutput out = new OutputStreamDataOutput(new FileOutputStream(file));
        fst.save(out);
        out.close();
    }
}
