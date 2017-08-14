package io.cattle.platform.eventing.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import javax.xml.bind.annotation.XmlTransient;

public interface Event {

    String TRANSITIONING_YES = "yes";
    String TRANSITIONING_NO = "no";
    String TRANSITIONING_ERROR = "error";

    String REPLY_PREFIX = "reply.";
    String REPLY_SUFFIX = ".reply";

    String getId();

    String getName();

    @JsonInclude(Include.NON_EMPTY)
    String getReplyTo();

    @XmlTransient
    Object getRequestData();

    String getResourceId();

    String getResourceType();

    @JsonInclude(Include.NON_EMPTY)
    String getPreviousId();

    @JsonInclude(Include.NON_EMPTY)
    String getTransitioning();

    @JsonInclude(Include.NON_EMPTY)
    String getTransitioningMessage();

    Object getData();

    @JsonInclude(Include.NON_EMPTY)
    Long getTime();

    @JsonInclude(Include.NON_EMPTY)
    Long getTimeoutMillis();
}
