package io.cattle.platform.agent.server.ping.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingStatus {

    private static final Logger log = LoggerFactory.getLogger(PingStatus.class);

    long agentId;
    int failures = 0;

    public PingStatus(long agentId) {
        super();
        this.agentId = agentId;
    }

    public void success() {
        if (failures > 0) {
            log.info("Recieved ping for [{}]", agentId);
        }
        failures = 0;
    }

    public int failed() {
        return ++failures;
    }

}
