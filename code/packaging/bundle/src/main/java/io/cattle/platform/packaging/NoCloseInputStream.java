package io.cattle.platform.packaging;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class NoCloseInputStream extends FilterInputStream {
    protected NoCloseInputStream(InputStream in) {
        super(in);
    }

    @Override
    public void close() throws IOException {
    }
}
