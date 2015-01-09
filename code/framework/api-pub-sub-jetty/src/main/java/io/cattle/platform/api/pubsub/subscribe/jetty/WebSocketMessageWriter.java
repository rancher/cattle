package io.cattle.platform.api.pubsub.subscribe.jetty;

import io.cattle.platform.api.pubsub.subscribe.MessageWriter;

import java.io.EOFException;
import java.io.IOException;

import org.eclipse.jetty.websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketMessageWriter implements WebSocket, MessageWriter {

    private static final Logger log = LoggerFactory.getLogger(WebSocketMessageWriter.class);

    Connection connection;

    @Override
    public void onOpen(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void onClose(int closeCode, String message) {
        log.debug("Websocket connection closed");
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

}