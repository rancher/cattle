package io.cattle.platform.core.dao;

import io.cattle.platform.core.addon.SecretReference;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Secret;

import java.util.List;
import java.util.Map;

public interface SecretDao {

    public class InstanceAndHost {
        public Instance instance;
        public Host host;

        public InstanceAndHost(Instance instance, Host host) {
            super();
            this.instance = instance;
            this.host = host;
        }
    }

    InstanceAndHost getHostForInstanceUUIDAndAuthAccount(long accountId, String instanceUuid);

    Map<Long, Secret> getSecrets(List<SecretReference> refs);

}
