package io.github.ibuildthecloud.dstack.eventing.impl;

import io.github.ibuildthecloud.dstack.eventing.EventListener;
import io.github.ibuildthecloud.dstack.eventing.PoolSpecificListener;
import io.github.ibuildthecloud.dstack.eventing.model.Event;

import com.google.common.util.concurrent.SettableFuture;

public class FutureEventListener implements EventListener, PoolSpecificListener {

    String replyTo;
    SettableFuture<Event> future;
    Event event;
    boolean failed;

    public FutureEventListener(String replyTo) {
        super();
        this.replyTo = replyTo;
    }

    @Override
    public void onEvent(Event reply) {
        if ( future != null && event != null ) {
            String[] previous = reply.getPreviousIds();
            if ( previous != null && previous.length > 0 && previous[0].equals(event.getId()) ) {
                future.set(event);
            }
        }
    }

    public void reset() {
        future = null;
        event = null;
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
    public boolean isAllowQueueing() {
        return true;
    }

    @Override
    public String getPoolKey() {
        return "reply";
    }

    @Override
    public int getQueueDepth() {
        return 1000;
    }

}
