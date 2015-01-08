package io.cattle.platform.archaius.eventing.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cattle.platform.archaius.eventing.ArchaiusEventListener;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.util.type.CollectionUtils;

public class ArchaiusEventListenerImpl implements ArchaiusEventListener {

    private static final Logger log = LoggerFactory.getLogger(ArchaiusEventListenerImpl.class);

    @Override
    public void apiChange(Event event) {
        Map<String, Object> data = CollectionUtils.toMap(event.getData());
        if ("activeSetting".equals(data.get("type"))) {
            log.info("Refreshing settings");
            ArchaiusUtil.refresh();
        }
    }

}
