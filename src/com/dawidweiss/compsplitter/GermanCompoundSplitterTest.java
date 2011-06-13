package com.dawidweiss.compsplitter;

import java.io.IOException;

import org.junit.Test;

/**
 * Run the decompounder on a provided list of known splits.
 */
public class GermanCompoundSplitterTest
{
    @Test
    public void runOnKnownSplits()
    {
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

            // these seem to be lexicalized in surfaceForms
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
