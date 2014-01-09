package io.github.ibuildthecloud.dstack.archaius.eventing.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ibuildthecloud.dstack.archaius.eventing.ArchaiusEventListener;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.util.type.CollectionUtils;

public class ArchaiusEventListenerImpl implements ArchaiusEventListener {

    private static final Logger log = LoggerFactory.getLogger(ArchaiusEventListenerImpl.class);

    @Override
    public void apiChange(Event event) {
        Map<String,Object> data = CollectionUtils.toMap(event.getData());
        if ( "activeSetting".equals(data.get("type")) ) {
            log.info("Refreshing settings");
            ArchaiusUtil.refresh();
        }
    }

}
