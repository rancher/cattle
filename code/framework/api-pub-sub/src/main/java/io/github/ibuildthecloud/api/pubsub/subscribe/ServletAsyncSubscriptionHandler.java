package io.github.ibuildthecloud.api.pubsub.subscribe;

import io.github.ibuildthecloud.dstack.async.retry.RetryTimeoutService;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletAsyncSubscriptionHandler extends NonBlockingSubscriptionHandler {

    public ServletAsyncSubscriptionHandler(JsonMapper jsonMapper, EventService eventService,
            RetryTimeoutService retryTimeout, ExecutorService executorService) {
        super(jsonMapper, eventService, retryTimeout, executorService);
    }

    @Override
    protected OutputStream getOutputStream(ApiRequest apiRequest) throws IOException {
        apiRequest.commit();

        HttpServletRequest request = apiRequest.getRequestServletContext().getRequest();
        HttpServletResponse response = apiRequest.getRequestServletContext().getResponse();

        AsyncContext ctx = request.startAsync(request, response);
        ctx.setTimeout(0);

        return ctx.getResponse().getOutputStream();

    }

}
