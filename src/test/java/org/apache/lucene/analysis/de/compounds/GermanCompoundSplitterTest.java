package org.apache.lucene.analysis.de.compounds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.de.compounds.GermanCompoundSplitter;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Run the decompounder on a provided list of known splits.
 */
public class GermanCompoundSplitterTest
{
    private GermanCompoundSplitter splitter = new GermanCompoundSplitter();

    @DataProvider(name = "compounds", parallel = false)
    public String [][] createCompoundList() throws IOException
    {
        List<String[]> input = new ArrayList<String[]>();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                this.getClass().getClassLoader()
                    .getResourceAsStream("test-compounds.utf8"), "UTF-8"));
        
        String line;
        while ((line = reader.readLine()) != null)
        {
            final int hashIndex = line.indexOf('#'); 
            if (hashIndex >= 0)
                line = line.substring(0, hashIndex);

            line = line.trim();
            if (line.isEmpty())
                continue;
            
            String [] components = line.split("\\s+");
            if (components.length != 2)
                throw new IOException("Expected two columns in line: "
                    + line);
            input.add(components);
        }

        reader.close();
        
        return input.toArray(new String[input.size()][]);
    }

    @Test(dataProvider = "compounds")
    public void checkCompound(String compound, String splits)
    {
        CharSequence split = splitter.split(compound);
        Assert.assertEquals(split == null ? compound : split.toString(), splits);
    }
}
