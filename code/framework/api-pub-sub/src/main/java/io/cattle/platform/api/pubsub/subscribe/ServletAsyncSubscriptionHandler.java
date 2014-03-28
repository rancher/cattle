package io.cattle.platform.api.pubsub.subscribe;

import io.cattle.platform.async.retry.RetryTimeoutService;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletAsyncSubscriptionHandler extends NonBlockingSubscriptionHandler {

    public ServletAsyncSubscriptionHandler(JsonMapper jsonMapper, EventService eventService,
            RetryTimeoutService retryTimeout, ExecutorService executorService,
            List<ApiPubSubEventPostProcessor> eventProcessors) {
        super(jsonMapper, eventService, retryTimeout, executorService, eventProcessors);
    }

    @Override
    protected MessageWriter getMessageWriter(ApiRequest apiRequest) throws IOException {
        apiRequest.commit();

        HttpServletRequest request = apiRequest.getServletContext().getRequest();
        HttpServletResponse response = apiRequest.getServletContext().getResponse();

        AsyncContext ctx = request.startAsync(request, response);
        ctx.setTimeout(0);

        return new OutputStreamMessageWriter(ctx.getResponse().getOutputStream());
    }

}