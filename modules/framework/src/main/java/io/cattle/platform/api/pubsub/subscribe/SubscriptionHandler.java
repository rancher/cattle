package io.cattle.platform.api.pubsub.subscribe;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.util.Collection;

public interface SubscriptionHandler {

    boolean subscribe(Collection<String> eventNames, ApiRequest apiRequest, boolean strip) throws IOException;

}
