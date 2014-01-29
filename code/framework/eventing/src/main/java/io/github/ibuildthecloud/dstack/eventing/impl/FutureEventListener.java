package io.github.ibuildthecloud.dstack.eventing.impl;

import io.github.ibuildthecloud.dstack.eventing.EventListener;
import io.github.ibuildthecloud.dstack.eventing.EventProgress;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.PoolSpecificListener;
import io.github.ibuildthecloud.dstack.eventing.exception.EventExecutionException;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;

import com.google.common.util.concurrent.SettableFuture;

public class FutureEventListener implements EventListener, PoolSpecificListener {

    EventService eventService;
    String replyTo;
    SettableFuture<Event> future;
    EventProgress progress;
    Event event;
    boolean failed;

    public FutureEventListener(EventService eventService, String replyTo) {
        super();
        this.replyTo = replyTo;
        this.eventService = eventService;
    }

    @Override
    public synchronized void onEvent(Event reply) {
        if ( future != null && event != null ) {
            String[] previous = reply.getPreviousIds();

            if ( previous != null && previous.length > 0 && previous[0].equals(event.getId()) ) {
                EventVO<Object> replyWithName = new EventVO<Object>(reply);
                replyWithName.setName(appendReply(event.getName()));

                eventService.publish(replyWithName);

                String transitioning = replyWithName.getTransitioning();

                if ( transitioning == null || Event.TRANSITIONING_NO.equals(transitioning) ) {
                    future.set(replyWithName);
                } else if ( Event.TRANSITIONING_ERROR.equals(transitioning) ) {
                    future.setException(new EventExecutionException(replyWithName));
                } else if ( progress != null ){
                    progress.progress(replyWithName);
                }
            }
        }
    }

    protected String appendReply(String name) {
        int i = name.indexOf(";");
        if ( i == -1 ) {
            return name + Event.REPLY_SUFFIX;
        } else {
            return name.substring(0, i) + Event.REPLY_SUFFIX + name.substring(i);
        }

    }

    public synchronized void reset() {
        future = null;
        event = null;
        progress = null;
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

    public EventProgress getProgress() {
        return progress;
    }

    public void setProgress(EventProgress progress) {
        this.progress = progress;
    }

}
