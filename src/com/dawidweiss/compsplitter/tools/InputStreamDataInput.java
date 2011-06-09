package com.dawidweiss.compsplitter.tools;

import java.io.*;

import org.apache.lucene.store.DataInput;

import com.sun.xml.internal.rngom.parse.compact.EOFException;

/**
 * A {@link DataInput} wrapping a plain {@link InputStream}.
 */
public class InputStreamDataInput extends DataInput implements Closeable
{

    private final InputStream is;

    public InputStreamDataInput(InputStream is)
    {
        this.is = is;
    }

    @Override
    public void close() throws IOException
    {
        this.is.close();
    }

    @Override
    public byte readByte() throws IOException
    {
        int v = is.read();
        if (v == -1) throw new EOFException();
        return (byte) v;
    }

    @Override
    public void readBytes(byte[] b, int offset, int len) throws IOException
    {
        while (len > 0) {
            final int cnt = is.read(b, offset, len);
            len -= cnt;
            offset += cnt;
        }
    }
}
