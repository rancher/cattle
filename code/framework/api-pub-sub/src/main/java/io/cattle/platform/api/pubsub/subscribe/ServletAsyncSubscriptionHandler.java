package io.cattle.platform.api.pubsub.subscribe;

import io.cattle.platform.archaius.util.ArchaiusUtil;
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

import com.netflix.config.DynamicIntProperty;

public class ServletAsyncSubscriptionHandler extends NonBlockingSubscriptionHandler {

    public static final DynamicIntProperty TIMEOUT = ArchaiusUtil.getInt("api.pub.sub.servlet.timeout.ms");

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
        ctx.setTimeout(TIMEOUT.get());

        return new AsyncOutputStreamMessageWriter(ctx.getResponse().getOutputStream(), ctx);
    }

}