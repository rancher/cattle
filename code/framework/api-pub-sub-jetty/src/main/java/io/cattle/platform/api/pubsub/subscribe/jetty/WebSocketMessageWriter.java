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

    Connection connection;
    Session session;
    @Override
    public void onWebSocketConnect(Session session) {
        this.connection = connection;
    }

    @Override
    public void onWebSocketClose(int closeCode, String message) {
        log.info("Websocket connection closed");
        connection = null;
    }


    @Override
    public void write(String message, Object writeLock) throws IOException {
        if (connection == null) {
            throw new EOFException("WebSocket is closed");
        } else {
            connection.sendMessage(message);
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            connection.close();
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