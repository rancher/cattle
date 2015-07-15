package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerListener;

import java.util.List;

public interface LoadBalancerDao {

    boolean updateLoadBalancer(long lbId, Long glbId, Long weight);

    List<? extends LoadBalancer> listByConfigId(long configId);

    List<? extends LoadBalancerListener> listActiveListenersForConfig(long configId);

    LoadBalancer getActiveLoadBalancerById(long lbId);
    
    void addListenerToConfig(final LoadBalancerConfig config, final long listenerId);

    List<Certificate> getLoadBalancerCertificates(LoadBalancer lb);

    Certificate getLoadBalancerDefaultCertificate(LoadBalancer lb);
}
