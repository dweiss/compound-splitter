package com.dawidweiss.compsplitter.tools;

import java.io.*;

import org.apache.lucene.util.fst.*;
import org.apache.lucene.util.fst.FST.INPUT_TYPE;

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

        final Builder<Object> builder = new Builder<Object>(INPUT_TYPE.BYTE4, NoOutputs.getSingleton());
        final Object nothing = NoOutputs.getSingleton().getNoOutput();
        String line, last = null;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.equals(last))
                continue;
            last = line;
            builder.add(line, nothing);
        }
        final FST<Object> fst = builder.finish();

        final OutputStreamDataOutput out = new OutputStreamDataOutput(new FileOutputStream("dict-inverted.fst"));
        fst.save(out);
        out.close();
    }
}
