package io.github.ibuildthecloud.dstack.packaging;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NoCloseOutputStream extends FilterOutputStream {
    protected NoCloseOutputStream(OutputStream in) {
        super(in);
    }

    @Override
    public void close() throws IOException {
    }
}

