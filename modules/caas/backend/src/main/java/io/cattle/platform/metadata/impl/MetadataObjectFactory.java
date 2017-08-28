package io.cattle.platform.metadata.impl;

import io.cattle.platform.core.addon.metadata.EnvironmentInfo;
import io.cattle.platform.core.addon.metadata.HostInfo;
import io.cattle.platform.core.addon.metadata.InstanceInfo;
import io.cattle.platform.core.addon.metadata.MetadataObject;
import io.cattle.platform.core.addon.metadata.NetworkInfo;
import io.cattle.platform.core.addon.metadata.ServiceInfo;
import io.cattle.platform.core.addon.metadata.StackInfo;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Set;

public class MetadataObjectFactory {

    private static Set<String> ACCOUNT_KINDS = CollectionUtils.set(
            AccountConstants.TYPE,
            ProjectConstants.TYPE);

    public MetadataObject convert(Object obj) {
        if (obj instanceof Host) {
            return new HostInfo((Host) obj);
        } else if (obj instanceof Instance) {
            return new InstanceInfo((Instance) obj);
        } else if (obj instanceof Stack) {
            return new StackInfo(((Stack)obj));
        } else if (obj instanceof Service) {
            return new ServiceInfo((Service) obj);
        } else if (obj instanceof Network) {
            return new NetworkInfo((Network) obj);
        } else if (obj instanceof Account) {
            if (ACCOUNT_KINDS.contains(((Account) obj).getKind())) {
                return new EnvironmentInfo((Account) obj);
            }
        }
        return null;
    }

}
