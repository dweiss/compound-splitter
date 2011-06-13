package com.dawidweiss.compsplitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Run the decompounder on a provided list of known splits.
 */
public class GermanCompoundSplitterTest
{
    private GermanCompoundSplitter splitter = new GermanCompoundSplitter();
    
    @DataProvider(name = "compounds")
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
        Assert.assertEquals(split == null ? null : split.toString(), splits);
    }

    
    public static void main(String [] args) throws IOException
    {
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
            "Monats-bericht", "Entwicklungs-projekt", "Unternehmens-kultur",
            "Bevölkerungs-wachstum", "Reichstags-abgeordneter", "Städte-partnerschaft",
            "Auftrags-eingang",

            "zeitschriften-redaktionen", "Gebrauchtwagen", "Hochbahn", "Gebrauchtwagen",
            "Höher-versicherung",

            "Sünder-ecke",

            // these seem to be lexicalized in surfaceForms
            "Abteil-ende", "Außen-stürmer", "Beileids-karte", "Comic-strip",
            "Eichen-seide", "Flügel-stürmer",

            "versandabteilungen", "abteilungsleiterposition", "Ausstrahlungsnotizen",

            "Stadt-plan-ersatz", "Konsumentenbeschimpfung", "puffer-bau-stein",
            "gelbrot",
        };

        GermanCompoundSplitter splitter = new GermanCompoundSplitter();
        for (String s : inputs)
        {
            System.out.println("#" + s);
            s = s.toLowerCase().replace("-", "");
            System.out.println(splitter.split(s));
            System.out.println("------");
        }
    }
}
