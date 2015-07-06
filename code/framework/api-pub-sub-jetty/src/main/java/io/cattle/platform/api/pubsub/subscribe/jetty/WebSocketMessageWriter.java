package io.cattle.platform.api.pubsub.subscribe.jetty;

import io.cattle.platform.api.pubsub.subscribe.MessageWriter;

import java.io.EOFException;
import java.io.IOException;

import org.eclipse.jetty.websocket.WebSocket.Connection;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketMessageWriter extends WebSocketAdapter implements MessageWriter {

    private static final Logger log = LoggerFactory.getLogger(WebSocketMessageWriter.class);

    Session session;
    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;
    }

    @Override
    public void onWebSocketClose(int closeCode, String message) {
        log.info("Websocket connection closed");
        session = null;
    }


    @Override
    public void write(String message, Object writeLock) throws IOException {
        if (session == null) {
            throw new EOFException("WebSocket is closed");
        } else {
            session.getRemote().sendString(message);
        }
    }

    @Override
    public void close() {
        if (session != null) {
            session.close();
        }
    }
/*
    @Override
    public void onMessage(String data) {
        log.info("mersgage " + data);
    }

    @Override
    public boolean onControl(byte controlCode, byte[] data, int offset, int length) {
        log.info("mersgage " + controlCode + " | " + data);
    }
    */
}