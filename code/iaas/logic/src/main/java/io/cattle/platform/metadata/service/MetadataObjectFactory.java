package io.cattle.platform.metadata.service;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.metadata.model.InstanceInfo;
import io.cattle.platform.metadata.model.HostInfo;
import io.cattle.platform.metadata.model.MetadataObject;
import io.cattle.platform.metadata.model.NetworkInfo;
import io.cattle.platform.metadata.model.ServiceInfo;

public class MetadataObjectFactory {

    public MetadataObject convert(Object obj) {
        if (obj instanceof Host) {
            return new HostInfo((Host) obj);
        } else if (obj instanceof Instance) {
            return new InstanceInfo((Instance) obj);
        } else if (obj instanceof Service) {
            return new ServiceInfo((Service) obj);
        } else if (obj instanceof Network) {
            return new NetworkInfo((Network) obj);
        }
        return null;
    }

}
