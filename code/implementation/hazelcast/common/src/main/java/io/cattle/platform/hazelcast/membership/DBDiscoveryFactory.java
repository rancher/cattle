package io.cattle.platform.hazelcast.membership;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryStrategyFactory;

public class DBDiscoveryFactory implements DiscoveryStrategyFactory {

    @Inject
    DBDiscovery discovery;

    @Override
    public Class<? extends DiscoveryStrategy> getDiscoveryStrategyType() {
        return DBDiscovery.class;
    }

    @Override
    public DiscoveryStrategy newDiscoveryStrategy(DiscoveryNode discoveryNode, ILogger logger,
            @SuppressWarnings("rawtypes") Map<String, Comparable> properties) {
        discovery.setSelfNode(discoveryNode);
        try {
            discovery.checkin();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return discovery;
    }

    @Override
    public Collection<PropertyDefinition> getConfigurationProperties() {
        return Collections.emptySet();
    }

}
