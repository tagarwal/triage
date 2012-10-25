package oracle.util.triage;

import java.io.*;

public class LimitInputStream extends FilterInputStream {

    protected LimitInputStream(InputStream in, long limit) {
        super(in);
        this.limit = limit;
    }

    private long position = 0l;
    private long limit = 0l;

    public int read() throws java.io.IOException {
        if (position > limit) {
            return -1;
        } else {
            position++; 
            return super.read();
        }
    }

    public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }

    public int read(byte[] buf, int start, int length) throws java.io.IOException {
      if (position > limit) {
          return -1;
      } else {
          int res = super.read(buf, start, length);
          if (res > 0) {
              position += res;
          }
          return res;
      }
    }
        
    public long skip(long delta) throws java.io.IOException {
        if (position > limit) {
            return -1l;
        } else {
            long res = super.skip(delta);
            if (res > 0l) {
                position += res;
            }
            return res;
        }
    }
}
