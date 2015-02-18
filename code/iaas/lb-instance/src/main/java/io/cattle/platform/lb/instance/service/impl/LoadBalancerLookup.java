package io.cattle.platform.lb.instance.service.impl;

import java.util.Set;

public interface LoadBalancerLookup {
    Set<Long> getLoadBalancerIds(Object obj);
}
