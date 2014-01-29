package io.github.ibuildthecloud.api.pubsub.subscribe;

import io.github.ibuildthecloud.api.pubsub.subscribe.MessageWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class OutputStreamMessageWriter implements MessageWriter {

    OutputStream os;

    public OutputStreamMessageWriter(OutputStream os) {
        super();
        this.os = os;
    }

    @Override
    public void write(String content, Object writeLock) throws IOException {
        synchronized (writeLock) {
            try {
                os.write((content + "\n\n").getBytes("UTF-8"));
                os.flush();
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
    }


}
