package io.cattle.platform.eventing.impl;

import com.google.common.util.concurrent.SettableFuture;
import io.cattle.platform.async.retry.Retry;
import io.cattle.platform.eventing.EventListener;
import io.cattle.platform.eventing.EventProgress;
import io.cattle.platform.eventing.PoolSpecificListener;
import io.cattle.platform.eventing.exception.EventExecutionException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;

public class FutureEventListener implements EventListener, PoolSpecificListener {

    AbstractEventService eventService;
    String replyTo;
    SettableFuture<Event> future;
    EventProgress progress;
    Event event;
    boolean failed;
    Retry retry;

    public FutureEventListener(AbstractEventService eventService, String replyTo) {
        super();
        this.replyTo = replyTo;
        this.eventService = eventService;
    }

    @Override
    public synchronized void onEvent(Event reply) {
        if (future != null && event != null) {
            String previous = reply.getPreviousId();

            if (!event.getId().equals(reply.getPreviousId())) {
                return;
            }

            EventVO<Object, Object> finalReply = new EventVO<Object, Object>(reply)
                    .withName(appendReply(event.getName()))
                    .withResourceId(null) // clear resourceId/Type because they can't be trusted
                    .withResourceType(null)
                    .withRequestData(event.getRequestData());

            String transitioning = finalReply.getTransitioning();

            if (transitioning == null || Event.TRANSITIONING_NO.equals(transitioning)) {
                future.set(finalReply);
            } else if (Event.TRANSITIONING_ERROR.equals(transitioning)) {
                future.setException(EventExecutionException.fromEvent(finalReply));
            } else if (progress != null) {
                if (retry != null) {
                    retry.setKeepalive(true);
                }
                progress.progress(finalReply);
            }
        }
    }

    protected String appendReply(String name) {
        int i = name.indexOf(";");
        if (i == -1) {
            return name + Event.REPLY_SUFFIX;
        } else {
            return name.substring(0, i) + Event.REPLY_SUFFIX + name.substring(i);
        }

    }

    public synchronized void reset() {
        future = null;
        event = null;
        progress = null;
        retry = null;
    }

    public SettableFuture<Event> getFuture() {
        return future;
    }

    public void setFuture(SettableFuture<Event> future) {
        this.future = future;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getReplyTo() {
        return replyTo;
    }

    @Override
    public String getPoolKey() {
        return "reply";
    }

    public EventProgress getProgress() {
        return progress;
    }

    public void setProgress(EventProgress progress) {
        this.progress = progress;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

}