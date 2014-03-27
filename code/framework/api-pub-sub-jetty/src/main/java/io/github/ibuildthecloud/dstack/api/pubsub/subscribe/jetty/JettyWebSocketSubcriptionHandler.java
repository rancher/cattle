package io.github.ibuildthecloud.dstack.api.pubsub.subscribe.jetty;

import io.github.ibuildthecloud.dstack.api.pubsub.subscribe.ApiPubSubEventPostProcessor;
import io.github.ibuildthecloud.dstack.api.pubsub.subscribe.MessageWriter;
import io.github.ibuildthecloud.dstack.api.pubsub.subscribe.NonBlockingSubscriptionHandler;
import io.github.ibuildthecloud.dstack.async.retry.RetryTimeoutService;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketFactory;
import org.eclipse.jetty.websocket.WebSocketFactory.Acceptor;

public class JettyWebSocketSubcriptionHandler extends NonBlockingSubscriptionHandler {

    public JettyWebSocketSubcriptionHandler() {
        super();
        setSupportGet(true);
    }

    public JettyWebSocketSubcriptionHandler(JsonMapper jsonMapper, EventService eventService,
            RetryTimeoutService retryTimeout, ExecutorService executorService,
            List<ApiPubSubEventPostProcessor> eventProcessors) {
        super(jsonMapper, eventService, retryTimeout, executorService, eventProcessors);
        setSupportGet(true);
    }

    @Override
    protected MessageWriter getMessageWriter(ApiRequest apiRequest) throws IOException {
        HttpServletRequest req = apiRequest.getServletContext().getRequest();
        HttpServletResponse resp = apiRequest.getServletContext().getResponse();
        final WebSocketMessageWriter messageWriter = new WebSocketMessageWriter();

        WebSocketFactory factory = new WebSocketFactory(new Acceptor() {
            @Override
            public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
                return messageWriter;
            }

            @Override
            public boolean checkOrigin(HttpServletRequest request, String origin) {
                return true;
            }
        });

        if ( factory.acceptWebSocket(req, resp) ) {
            apiRequest.commit();
            return messageWriter;
        } else {
            return null;
        }
    }

}