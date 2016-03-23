package io.cattle.platform.agent.impl;

import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.EventProgress;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.RetryCallback;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class EventCallProgressHelper {

    public static <T extends Event> ListenableFuture<T> call(final EventService eventService, final Event request, final Class<? extends T> reply,
            EventCallOptions options, final EventResponseMarshaller converter) {
        final EventProgress progress = options.getProgress();

        if (progress != null) {
            EventProgress newProgress = new EventProgress() {
                @Override
                public void progress(Event progressEvent) {
                    T result = converter.convert(progressEvent, reply);

                    if (result instanceof Event) {
                        progress.progress(result);
                    }
                }
            };

            options.setProgress(newProgress);
        }

        final RetryCallback retryCallback = options.getRetryCallback();

        if (retryCallback != null) {
            RetryCallback newCallback = new RetryCallback() {
                @Override
                public Event beforeRetry(Event event) {
                    Object data = event.getData();

                    if (data instanceof Event) {
                        data = retryCallback.beforeRetry((Event) data);
                        EventVO<Object> newEvent = new EventVO<Object>(event);
                        newEvent.setData(data);
                        event = newEvent;
                    }

                    return event;
                }
            };

            options.setRetryCallback(newCallback);
        }

        ListenableFuture<Event> future = eventService.call(request, options);
        return Futures.transform(future, new Function<Event, T>() {
            @Override
            public T apply(Event input) {
                return converter.convert(input, reply);
            }
        });
    }
}
