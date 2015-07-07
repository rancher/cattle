package io.cattle.platform.api.pubsub.subscribe.jetty;

import io.cattle.platform.api.pubsub.subscribe.MessageWriter;

import java.io.EOFException;
import java.io.IOException;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketMessageWriter extends WebSocketAdapter implements MessageWriter {

    private static final Logger log = LoggerFactory.getLogger(WebSocketMessageWriter.class);

    Session session;
    boolean connectionClosed = false;

    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;
    }

    @Override
    public void onWebSocketClose(int closeCode, String message) {
        log.debug("Websocket connection closed");
        session = null;
        connectionClosed = true;
    }

    @Override
    public void write(String message, Object writeLock) throws IOException {
        if (connectionClosed) {
            throw new EOFException("WebSocket is closed");
        }
        if (session != null) {
            session.getRemote().sendString(message);
        }
    }

    @Override
    public void close() {
        connectionClosed = true;
        if (session != null) {
            session.close();
        }
    }
}