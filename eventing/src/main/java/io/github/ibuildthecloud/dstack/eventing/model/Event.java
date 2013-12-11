package io.github.ibuildthecloud.dstack.eventing.model;

import java.util.Date;

public interface Event {

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
