package io.cattle.platform.api.pubsub.subscribe.jetty;

import io.cattle.platform.api.pubsub.subscribe.MessageWriter;

import java.io.IOException;

import org.eclipse.jetty.websocket.WebSocket;

public class WebSocketMessageWriter implements WebSocket, MessageWriter {

    Connection connection;

    @Override
    public void onOpen(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void onClose(int closeCode, String message) {
        connection = null;
    }

    @Override
    public void write(String message, Object writeLock) throws IOException {
        if ( connection == null ) {
            throw new IOException("WebSocket is closed");
        } else {
            connection.sendMessage(message);
        }
    }

}
