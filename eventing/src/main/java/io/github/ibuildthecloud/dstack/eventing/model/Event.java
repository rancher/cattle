package io.github.ibuildthecloud.dstack.eventing.model;

import java.util.Date;

public interface Event {

    public static final String REPLY_PREFIX  = "reply.";
    public static final String REPLY_SUFFIX  = ".reply";

    String getId();

    String getName();

    String getReplyTo();

    String getResourceId();

    String getResourceType();

    String[] getPreviousIds();

    Object getData();

    Date getTime();

    String getPublisher();

}
