package io.cattle.platform.api.pubsub.subscribe;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

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
        if (os == null) {
            throw new EOFException("OutputStream is closed");
        }

        synchronized (writeLock) {
            try {
                byte[] bytes = content.getBytes("UTF-8");
                os.write(bytes);

                /*
                 * Why would we do such a stupid and wasteful thing? Because I
                 * couldn't figure out how to stream lines in Python requests.
                 * The builtin iter_lines() method will chunk on 512 bytes. But
                 * it will block until it gets 512. So it can not get the \n
                 * because the buffer isn't full yet. So we send garbage to make
                 * the buffer full. I tried setting the chunk size to 1, but
                 * caused bad performance issues.
                 */
                byte[] fill = new byte[512];
                Arrays.fill(fill, (byte) ' ');
                fill[0] = '\n';
                fill[511] = '\n';
                os.write(fill);

                os.flush();
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void close() {
        if (os != null) {
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