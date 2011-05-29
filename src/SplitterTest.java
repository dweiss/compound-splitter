import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import de.abelssoft.wordtools.jWordSplitter.impl.GermanWordSplitter;

public class SplitterTest
{
    public static interface Decompounder
    {
        public String split(String in);
    }

    public static class JWordSplitterDecompounder implements Decompounder
    {
        private GermanWordSplitter wordSplitter;

        JWordSplitterDecompounder()
        {
            try
            {
                wordSplitter = new GermanWordSplitter(false);
                wordSplitter.setStrictMode(true);
                wordSplitter.setMinimumWordLength(3);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String split(String in)
        {
            StringBuilder b = new StringBuilder();
            for (String s : wordSplitter.splitWord(in))
            {
                if (b.length() > 0) b.append("+");
                b.append(s);
            }

            return b.toString();
        }
    }

    public static void main(String [] args) throws Exception
    {
        Decompounder decompounder = new JWordSplitterDecompounder();

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            cl.getResourceAsStream("ccorpus.txt"), "UTF-8"));

        int instances = 0;
        int correct = 0;

        String line;
        while ((line = reader.readLine()) != null)
        {
            String [] parts = line.split("\\s");
            String compound = parts[0];

            String decomposed = parts[1];
            decomposed = decomposed.replaceAll("\\{[^\\}]+\\}", "");
            decomposed = decomposed.replaceAll("[\\(\\)]", "");
            decomposed = decomposed.replaceAll("\\,[a-z]+", "");
            decomposed = decomposed.replace("|", "");
            decomposed = decomposed.replace("U", "ü");
            decomposed = decomposed.replace("A", "ä");

            // mediengestalter
            // minimal+ausführung minima+laus+führung
            String result = decompounder.split(compound);

            instances++;
            if (result.equals(decomposed))
            {
                correct++;
            }
            else
            {
                System.out.println(decomposed + " " + result);
            }
        }

        System.out.println("Instances: " + instances);
        System.out.println("Correct: " + correct + " (" + (correct * 100.0 / instances)
            + "%)");
    }
}