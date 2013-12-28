package io.github.ibuildthecloud.api.pubsub.subscribe;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.util.List;

public interface SubscriptionHandler {

    boolean subscribe(List<String> eventNames, ApiRequest apiRequest, boolean strip) throws IOException;

}
