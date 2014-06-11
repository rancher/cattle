package io.cattle.platform.eventing;

import io.cattle.platform.eventing.model.Event;

public interface RetryCallback {

    Event beforeRetry(Event event);

}
