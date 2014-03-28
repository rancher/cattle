package io.cattle.platform.api.pubsub.subscribe;

import io.cattle.platform.eventing.model.EventVO;

public interface ApiPubSubEventPostProcessor {

    void processEvent(EventVO<Object> event);

}
