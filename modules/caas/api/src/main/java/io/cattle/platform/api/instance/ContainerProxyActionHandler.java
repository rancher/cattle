package io.cattle.platform.api.instance;

import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.api.model.ContainerProxy;
import io.cattle.platform.docker.api.model.HostAccess;
import io.cattle.platform.docker.util.DockerUtils;
import io.cattle.platform.hostapi.HostApiAccess;
import io.cattle.platform.hostapi.HostApiService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class ContainerProxyActionHandler implements ActionHandler {

    private static final DynamicStringProperty HOST_PROXY_PATH = ArchaiusUtil.getString("host.proxy.path");
    private static final DynamicLongProperty EXPIRE_SECONDS = ArchaiusUtil.getLong("host.proxy.jwt.expiration.seconds");
    private static final Set<String> VALID_SCHEMES = CollectionUtils.set("http", "https");

    HostApiService apiService;
    ObjectManager objectManager;

    public ContainerProxyActionHandler(HostApiService apiService, ObjectManager objectManager) {
        this.apiService = apiService;
        this.objectManager = objectManager;
    }

    @Override
    public Object perform(Object obj, ApiRequest request) {
        Host host = null;
        Instance container = null;

        if (obj instanceof Instance) {
            container = (Instance) obj;
            host = DockerUtils.getHostFromContainer(objectManager, container, null);
        }

        if (host == null) {
            return null;
        }

        ContainerProxy proxy = request.proxyRequestObject(ContainerProxy.class);

        String dockerId = DockerUtils.getDockerIdentifier(container);
        String ipAddress = DataAccessor.fieldString(container, InstanceConstants.FIELD_PRIMARY_IP_ADDRESS);
        if (ipAddress == null) {
            return null;
        }

        Map<String, Object> labels = DataAccessor.fieldMapRO(container, InstanceConstants.FIELD_LABELS);
        String port = ObjectUtils.toString(labels.get(SystemLabels.LABEL_PROXY_PORT));
        String scheme = ObjectUtils.toString(labels.get(SystemLabels.LABEL_PROXY_SCHEME));
        if (!VALID_SCHEMES.contains(scheme)) {
            scheme = null;
        }

        Map<String, Object> data = CollectionUtils.asMap(InstanceConstants.DOCKER_CONTAINER, dockerId,
                "scheme", StringUtils.isBlank(scheme) ? proxy.getScheme() : scheme,
                "address", ipAddress + ":" + (StringUtils.isBlank(port) ? proxy.getPort() : port));

        Date expiration = new Date(System.currentTimeMillis() + EXPIRE_SECONDS.get() * 1000);

        HostApiAccess apiAccess = apiService.getAccess(request, host.getId(), CollectionUtils.asMap("proxy", data),
                expiration, HOST_PROXY_PATH.get());

        if (apiAccess == null) {
            return null;
        }

        HostAccess access = new HostAccess(apiAccess.getUrl().replaceFirst("ws", "http"), apiAccess.getAuthenticationToken());
        return access;
    }

}
