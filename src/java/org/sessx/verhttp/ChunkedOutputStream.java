package org.sessx.verhttp;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


public class ChunkedOutputStream extends FilterOutputStream {

    public ChunkedOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.writeBufsingl();
        this.writeChunk(b, off, len);
    }
    
    private synchronized void writeChunk(byte[] b, int off, int len)
            throws IOException
    {
        Objects.checkFromIndexSize(off, len, b.length);
        super.out.write(Integer.toHexString(len)
                               .getBytes(StandardCharsets.UTF_8));
        super.out.write(CRLF);
        super.out.write(b, off, len);
        super.out.write(CRLF);
    }

    private void writeBufsingl() throws IOException {
        synchronized(this.bufsingl) {
            if(this.bufindex != 0) {
                this.writeChunk(this.bufsingl, 0, this.bufindex);
                this.bufindex = 0;
            }
        }
    }

    @Override
    public void write(int b) {
        synchronized(this.bufsingl) {
            if(this.bufindex >= this.bufsingl.length) {
                byte[] nbuf = new byte[this.bufsingl.length + 8];
                for(int i = 0; i < this.bufsingl.length; i++) {
                    nbuf[i] = this.bufsingl[i];
                }
                this.bufsingl = nbuf;
            }
            this.bufsingl[bufindex++] = (byte)b;
        }
    }
    
    @Override
    public void flush() throws IOException {
        this.writeBufsingl();
        super.flush();
    }
    
    @Override
    public void close() throws IOException {
        this.writeBufsingl();
        super.close();
    }
    
    private byte[] bufsingl = new byte[64];
    private int    bufindex = 0;

    public static final byte[] CRLF = {(byte)'\r', (byte)'\n'};

}
