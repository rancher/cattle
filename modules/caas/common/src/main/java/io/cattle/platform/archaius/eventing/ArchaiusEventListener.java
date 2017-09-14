package io.cattle.platform.archaius.eventing;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchaiusEventListener implements AnnotatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(ArchaiusEventListener.class);

    @EventHandler
    public void settingsChange(Event event) {
        ArchaiusUtil.refresh();
    }

}

