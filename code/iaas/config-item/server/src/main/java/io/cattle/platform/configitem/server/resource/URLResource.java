package io.cattle.platform.configitem.server.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.NullOutputStream;

public class URLResource extends AbstractResource {

    URL url;
    long size;

    public URLResource(String name, URL url) {
        super(name);
        this.url = url;
        calculateSize();
    }

    protected void calculateSize() {
        InputStream is = null;
        CountingOutputStream os = null;
        try {
            os = new CountingOutputStream(new NullOutputStream());
            is = getInputStream();
            IOUtils.copy(is, os);

            size = os.getCount();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to count bytes for [" + url + "]", e);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return url.openStream();
    }

}
