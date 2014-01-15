package io.github.ibuildthecloud.dstack.eventing.model;

import java.util.Date;

public interface Event {

    public static final String TRANSITIONING_YES = "yes";
    public static final String TRANSITIONING_NO = "no";
    public static final String TRANSITIONING_ERROR = "error";

    public static final String REPLY_PREFIX  = "reply.";
    public static final String REPLY_SUFFIX  = ".reply";

    String getId();

    String getName();

    String getReplyTo();

    String getResourceId();

    String getResourceType();

    String[] getPreviousIds();

    String[] getPreviousNames();

    String getTransitioning();

    Integer getTransitioningProgress();

    String getTransitioningMessage();

    String getTransitioningInternalMessage();

    Object getData();

    Date getTime();

    String getPublisher();

}
