package org.apache.lucene.analysis.de.compounds;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.compound.CompoundWordTokenFilterBase;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GermanCompoundSplitterTokenFilter extends
    CompoundWordTokenFilterBase {
  private static Logger log = LoggerFactory
      .getLogger(GermanCompoundSplitterTokenFilter.class);

  private GermanCompoundSplitter splitter;
  
  public GermanCompoundSplitterTokenFilter(Version matchVersion,
      TokenStream input, String fstFile) {
    super(matchVersion, input, null);
    GermanCompoundSplitter.initFSTs(fstFile);
    this.splitter = new GermanCompoundSplitter();
  }
  
  public void decompose() {
    String splitWords = new String();
    String incomingPossibleCompound = termAtt.toString();
    try {
      CharSequence sw = splitter.split(incomingPossibleCompound);
      if (sw != null) {
        splitWords = sw.toString(); // supplywood
                                    // ->
                                    // supply.wood,sup.plywood
      } else {
        splitWords = incomingPossibleCompound;
      }
      String[] possibleSplits = splitWords.split(",");
      String[] words = possibleSplits[0].split("\\."); // Take the first
                                                       // suggestion
      for (String word : words) {
        int startInd = incomingPossibleCompound.indexOf(word);
        int length = word.length();
        tokens.add(new CompoundToken(startInd, length));
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }

  }
}
