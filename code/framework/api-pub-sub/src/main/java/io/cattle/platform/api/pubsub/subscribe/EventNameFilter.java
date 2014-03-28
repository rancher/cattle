package io.cattle.platform.api.pubsub.subscribe;

public interface EventNameFilter {

    String filterEventName(String name);

}
