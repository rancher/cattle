package io.cattle.platform.docker.util;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.constants.DockerHostConstants;
import io.cattle.platform.object.ObjectManager;

public class DockerUtils {

    public static Host getHostFromContainer(ObjectManager objectManager, Instance instance, String hostKind) {
        Host found = null;
        for ( Host host : objectManager.mappedChildren(instance, Host.class) ) {
            found = host;
        }

        if ( found != null ) {
            found = ApiUtils.getPolicy().authorizeObject(found);
        }

        if ( found == null ) {
            return null;
        }

        if ( hostKind != null && ! hostKind.equals(found.getKind()) ) {
            return null;
        }

        return found;
    }

    public static Host getHostFromContainer(ObjectManager objectManager, Instance instance) {
        return getHostFromContainer(objectManager, instance, DockerHostConstants.KIND_DOCKER);
    }

}
