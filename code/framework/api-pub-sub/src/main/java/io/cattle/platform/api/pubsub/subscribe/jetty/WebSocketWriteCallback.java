package io.cattle.platform.api.pubsub.subscribe.jetty;

import org.eclipse.jetty.websocket.api.WriteCallback;

public class WebSocketWriteCallback implements WriteCallback {

    WebSocketMessageWriter writer;

    public WebSocketWriteCallback(WebSocketMessageWriter writer) {
        this.writer = writer;
    }

    @Override
    public void writeFailed(Throwable e) {
        writer.close();
    }

    @Override
    public void writeSuccess() {
        writer.getQueuedMessageCount().decrementAndGet();
    }

}
