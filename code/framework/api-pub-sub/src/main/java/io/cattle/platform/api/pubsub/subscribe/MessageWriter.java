package io.cattle.platform.api.pubsub.subscribe;

import java.io.IOException;

public interface MessageWriter {

    void write(String message, Object writeLock) throws IOException;

    void close();

}
