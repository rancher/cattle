package io.cattle.platform.api.cluster;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.RegistrationToken;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Cluster;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.server.context.ServerContext.BaseProtocol;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

public class ClusterOutputFilter implements ResourceOutputFilter {

    private static final DynamicStringProperty DOCKER_CMD = ArchaiusUtil.getString("docker.register.command");
    private static final DynamicStringProperty WINDOWS_CMD = ArchaiusUtil.getString("windows.register.command");
    private static final DynamicStringProperty CLUSTER_CMD = ArchaiusUtil.getString("k8s.register.command");
    private static final DynamicStringProperty REQUIRED_IMAGE = ArchaiusUtil.getString("bootstrap.required.image");

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (!(original instanceof Cluster)) {
            return converted;
        }

        Cluster cluster = (Cluster) original;

        RegistrationToken regToken = DataAccessor.field(original, ClusterConstants.FIELD_REGISTRATION, RegistrationToken.class);
        if (regToken == null) {
            return converted;
        }

        String token = regToken.getToken();
        String hostUrl = regUrl(token);
        String clusterUrl = regUrl(token + ".yaml");

        String hostCommand = String.format(DOCKER_CMD.get(), REQUIRED_IMAGE.get(), hostUrl);
        String clusterCommand = String.format(CLUSTER_CMD.get(), clusterUrl);
        String windowsCommand = String.format(WINDOWS_CMD.get(), hostUrl);

        if (CommonStatesConstants.INACTIVE.equals(cluster.getState()) || cluster.getEmbedded()) {
            regToken.setHostCommand(hostCommand);
        }
        if (CommonStatesConstants.INACTIVE.equals(cluster.getState())) {
            regToken.setClusterCommand(clusterCommand);
        }
        regToken.setWindowsCommand(windowsCommand);
        regToken.setToken(token);
        regToken.setImage(REQUIRED_IMAGE.get());
        regToken.setRegistrationUrl(hostUrl);

        converted.getFields().put(ClusterConstants.FIELD_REGISTRATION, regToken);

        return converted;
    }

    private static String regUrl(String token) {
        if (ServerContext.isCustomApiHost()) {
            return ServerContext.getHostApiBaseUrl(BaseProtocol.HTTP) + "/scripts/" + token;
        } else {
            return ApiContext.getUrlBuilder().resourceReferenceLink("scripts", token).toExternalForm();
        }
    }

}
