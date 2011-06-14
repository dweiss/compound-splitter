package org.apache.lucene.analysis.de.compounds;

import java.io.*;

import org.apache.lucene.store.DataOutput;

/**
 * A {@link DataOutput} wrapping a plain {@link OutputStream}.
 */
class OutputStreamDataOutput extends DataOutput implements Closeable {
  
  private final OutputStream os;
  
  public OutputStreamDataOutput(OutputStream os) {
    this.os = os;
  }
  
  @Override
  public void writeByte(byte b) throws IOException {
    os.write(b);
  }
  
  @Override
  public void writeBytes(byte[] b, int offset, int length) throws IOException {
    os.write(b, offset, length);
  }
  
  @Override
  public void close() throws IOException
  {
      this.os.close();
  }
}
