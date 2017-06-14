package io.cattle.platform.api.pubsub.subscribe.jetty;

import io.cattle.platform.api.pubsub.subscribe.MessageWriter;
import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicIntProperty;

public class WebSocketMessageWriter extends WebSocketAdapter implements MessageWriter {

    private static final Logger log = LoggerFactory.getLogger(WebSocketMessageWriter.class);

    private static final DynamicIntProperty MAX_QUEUED_MESSAGES = ArchaiusUtil.getInt("subscribe.max.queued.messages");

    private Session session;
    private boolean connectionClosed = false;
    private AtomicInteger queuedMessageCount = new AtomicInteger();

    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;
    }

    @Override
    public void onWebSocketClose(int closeCode, String message) {
        connectionClosed = true;
        log.debug("Websocket connection closed. Code: [{}], message: [{}].", closeCode, message);
    }

    @Override
    public void write(String message, Object writeLock) throws IOException {
        // The explicit connnectionClosed check is because the session is null until the connection is initially established
        if (connectionClosed) {
            throw new EOFException("WebSocket is closed.");
        }

        if (queuedMessageCount.get() > MAX_QUEUED_MESSAGES.get()) {
            throw new IOException("Reached max queued messages [" + MAX_QUEUED_MESSAGES.get() + "].");
        }

        if (session != null && session.isOpen()) {
            try {
                session.getRemote().sendString(message, new WebSocketWriteCallback(this));
                queuedMessageCount.incrementAndGet();
            } catch (WebSocketException e) {
                // Thrown if getRemote() determines the connection was closed by the client. No need to log.
                close();
            }
        }
    }

    @Override
    public void close() {
        if (!connectionClosed) {
            if (session != null && session.isOpen()) {
                session.close();
            }
            connectionClosed = true;
        }
    }

    public AtomicInteger getQueuedMessageCount() {
        return queuedMessageCount;
    }

    public void setQueuedMessageCount(AtomicInteger queuedMessageCount) {
        this.queuedMessageCount = queuedMessageCount;
    }
}
