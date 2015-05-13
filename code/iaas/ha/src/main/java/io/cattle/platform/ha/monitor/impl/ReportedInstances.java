package io.cattle.platform.ha.monitor.impl;

import io.cattle.platform.ha.monitor.impl.ReportedInstance;

import java.util.HashMap;
import java.util.Map;

class ReportedInstances {

    String hostUuid;
    Map<String, ReportedInstance> byUuid = new HashMap<String, ReportedInstance>();
    Map<String, ReportedInstance> byExternalId = new HashMap<String, ReportedInstance>();
    
}
