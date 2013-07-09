package org.apache.lucene.analysis.de.compounds;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GermanCompoundSplitterTokenFilterFactory extends
    TokenFilterFactory {
  private static Logger log = LoggerFactory
      .getLogger(GermanCompoundSplitterTokenFilterFactory.class);

  private String dataDir;
  private String fstFile;

  @Override
  public void init(Map<String,String> args) {
    super.init(args);
    this.dataDir = args.get("dataDir");
    this.fstFile = dataDir + "words.fst";
    String[] inputFiles = {
dataDir + "morphy.txt",
        dataDir + "morphy-unknown.txt"};
    try {
      Boolean shouldCompileDict = Boolean.parseBoolean(args.get("compileDict"));
      if (shouldCompileDict) {
        CompileCompoundDictionaries.setDataDir(dataDir);
        CompileCompoundDictionaries.compile(inputFiles);
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

  @Override
  public TokenStream create(TokenStream input) {
    return new GermanCompoundSplitterTokenFilter(luceneMatchVersion, input,
        fstFile);
  }
}
