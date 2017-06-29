package io.cattle.platform.docker.util;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.constants.DockerHostConstants;
import io.cattle.platform.object.ObjectManager;

import org.apache.commons.lang3.StringUtils;

public class DockerUtils {

    public static Host getHostFromContainer(ObjectManager objectManager, Instance instance, String hostKind) {
        Host found = null;
        for (Host host : objectManager.mappedChildren(instance, Host.class)) {
            found = host;
        }

        if (found != null) {
            found = ApiUtils.getPolicy().authorizeObject(found);
        }

        if (found == null) {
            return null;
        }

        if (hostKind != null && !hostKind.equals(found.getKind())) {
            return null;
        }

        return found;
    }

    public static Host getHostFromContainer(ObjectManager objectManager, Instance instance) {
        return getHostFromContainer(objectManager, instance, DockerHostConstants.KIND_DOCKER);
    }

    /**
     * The docker API allows for identification of a container by name or id.
     * This method will return the docker container id (which is stored in the
     * externalId field of the instance) if it is available. If it isn't
     * available, the instance uuid will be returned, since that is currently
     * used as the name for docker containers that do not have the docker id
     * set.
     * 
     * @param instance
     * @return A string suitable for identifying a container in the docker API.
     */
    public static String getDockerIdentifier(Instance instance) {
        return !StringUtils.isEmpty(instance.getExternalId()) ? instance.getExternalId() : instance.getUuid();
    }
}
