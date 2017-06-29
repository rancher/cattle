package io.cattle.platform.eventing.model;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public interface Event {

    public static final String TRANSITIONING_YES = "yes";
    public static final String TRANSITIONING_NO = "no";
    public static final String TRANSITIONING_ERROR = "error";

    public static final String REPLY_PREFIX = "reply.";
    public static final String REPLY_SUFFIX = ".reply";

    String getId();

    String getName();

    @JsonInclude(Include.NON_EMPTY)
    String getReplyTo();

    String getResourceId();

    String getResourceType();

    @JsonInclude(Include.NON_EMPTY)
    String[] getPreviousIds();

    @JsonInclude(Include.NON_EMPTY)
    String[] getPreviousNames();

    @JsonInclude(Include.NON_EMPTY)
    String getTransitioning();

    @JsonInclude(Include.NON_EMPTY)
    Integer getTransitioningProgress();

    @JsonInclude(Include.NON_EMPTY)
    String getTransitioningMessage();

    @JsonInclude(Include.NON_EMPTY)
    String getTransitioningInternalMessage();

    Object getData();

    @JsonInclude(Include.NON_EMPTY)
    Date getTime();

    @JsonInclude(Include.NON_EMPTY)
    Long getTimeoutMillis();

    @JsonInclude(Include.NON_EMPTY)
    String getPublisher();

    @JsonInclude(Include.NON_EMPTY)
    Map<String, Object> getContext();
}
