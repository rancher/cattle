package io.cattle.platform.api.pubsub.subscribe.jetty;

import io.cattle.platform.api.pubsub.subscribe.ApiPubSubEventPostProcessor;
import io.cattle.platform.api.pubsub.subscribe.MessageWriter;
import io.cattle.platform.api.pubsub.subscribe.NonBlockingSubscriptionHandler;
import io.cattle.platform.async.retry.RetryTimeoutService;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class JettyWebSocketSubcriptionHandler extends NonBlockingSubscriptionHandler {

    public JettyWebSocketSubcriptionHandler(JsonMapper jsonMapper, EventService eventService, RetryTimeoutService retryTimeout, ExecutorService executorService,
            List<ApiPubSubEventPostProcessor> eventProcessors) {
        super(jsonMapper, eventService, retryTimeout, executorService, eventProcessors);
    }

    @Override
    protected MessageWriter getMessageWriter(ApiRequest apiRequest) throws IOException {
        HttpServletRequest req = apiRequest.getServletContext().getRequest();
        HttpServletResponse resp = apiRequest.getServletContext().getResponse();
        final WebSocketMessageWriter messageWriter = new WebSocketMessageWriter();
        WebSocketServerFactory factory = new WebSocketServerFactory();
        factory.getPolicy().setAsyncWriteTimeout(1000);
        factory.setCreator((servletReq, servletResp) -> messageWriter);

        if ("websocket".equalsIgnoreCase(req.getHeader("Upgrade")) && factory.acceptWebSocket(req, resp)) {
            apiRequest.setResponseCode(101);
            apiRequest.commit();
            return messageWriter;
        } else {
            return null;
        }
    }

}