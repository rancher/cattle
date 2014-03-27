package io.github.ibuildthecloud.dstack.api.pubsub.subscribe;

import io.github.ibuildthecloud.dstack.eventing.model.EventVO;

public interface ApiPubSubEventPostProcessor {

    void processEvent(EventVO<Object> event);

}
