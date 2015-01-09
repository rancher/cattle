package io.cattle.platform.docker.process.util;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.object.util.DataAccessor;

public class DockerProcessUtils {

    public static String getDockerIp(Instance instance) {
        return DataAccessor.fields(instance).withKey(DockerInstanceConstants.FIELD_DOCKER_IP).as(String.class);
    }

}
