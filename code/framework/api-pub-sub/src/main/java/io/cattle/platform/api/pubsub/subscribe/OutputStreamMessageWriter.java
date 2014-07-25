package io.cattle.platform.api.pubsub.subscribe;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputStreamMessageWriter implements MessageWriter {

    private static final Logger log = LoggerFactory.getLogger(OutputStreamMessageWriter.class);

    OutputStream os;

    public OutputStreamMessageWriter(OutputStream os) {
        super();
        this.os = os;
    }

    @Override
    public void write(String content, Object writeLock) throws IOException {
        if ( os == null ) {
            throw new EOFException("OutputStream is closed");
        }

        synchronized (writeLock) {
            try {
                os.write((content + "\n\n").getBytes("UTF-8"));
                os.flush();
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void close() {
        if ( os != null ) {
            try {
                os.close();
            } catch (IOException e) {
                log.info("Failed to close output stream for client : {}", e.getMessage());
            } finally {
                os = null;
            }
        }
    }

}