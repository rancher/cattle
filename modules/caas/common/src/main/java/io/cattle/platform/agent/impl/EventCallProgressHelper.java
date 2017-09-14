package io.cattle.platform.agent.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.EventProgress;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.RetryCallback;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;

public class EventCallProgressHelper {

    public static <T extends Event> ListenableFuture<T> call(final EventService eventService, final Event request, final Class<? extends T> reply,
            EventCallOptions options, final EventResponseMarshaller converter) {
        final EventProgress progress = options.getProgress();

        if (progress != null) {
            options.setProgress(progressEvent -> {
                T result = converter.convert(progressEvent, reply);
                if (result != null) {
                    progress.progress(result);
                }
            });
        }

        final RetryCallback retryCallback = options.getRetryCallback();

        if (retryCallback != null) {
            RetryCallback newCallback = event -> {
                Object data = event.getData();

                if (data instanceof Event) {
                    data = retryCallback.beforeRetry((Event) data);
                    EventVO<Object, Object> newEvent = new EventVO<>(event);
                    newEvent.setData(data);
                    event = newEvent;
                }

                return event;
            };

            options.setRetryCallback(newCallback);
        }

        ListenableFuture<Event> future = eventService.call(request, options);
        return Futures.transform(future, input -> converter.convert(input, reply));
    }
}
