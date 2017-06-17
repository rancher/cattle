package io.cattle.platform.api.serviceproxy;

import io.cattle.platform.api.instance.ContainerProxyActionHandler;
import io.cattle.platform.api.resource.AbstractNoOpResourceManager;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.addon.K8sClientConfig;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ClusterDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Cluster;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.docker.api.model.HostAccess;
import io.cattle.platform.docker.api.model.ServiceProxy;
import io.cattle.platform.hostapi.HostApiAccess;
import io.cattle.platform.hostapi.HostApiService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ReferenceValidator;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ServiceProxyManager extends AbstractNoOpResourceManager {

    ServiceDao serviceDao;
    ClusterDao clusterDao;
    ContainerProxyActionHandler actionHandler;
    ObjectManager objectManager;
    ReferenceValidator referenceValidator;
    HostApiService apiService;

    public ServiceProxyManager(ServiceDao serviceDao, ClusterDao clusterDao, ContainerProxyActionHandler actionHandler, ObjectManager objectManager, ReferenceValidator referenceValidator, HostApiService apiService) {
        this.serviceDao = serviceDao;
        this.clusterDao = clusterDao;
        this.actionHandler = actionHandler;
        this.objectManager = objectManager;
        this.referenceValidator = referenceValidator;
        this.apiService = apiService;
    }

    @Override
    public Object create(String type, ApiRequest request) {
        ServiceProxy proxy = request.proxyRequestObject(ServiceProxy.class);
        String serviceName = proxy.getService();
        if (StringUtils.isBlank(serviceName)) {
            request.setResponseCode(ResponseCodes.NOT_FOUND);
            return null;
        }

        String[] parts = StringUtils.split(serviceName, ".", 2);
        Service service = null;

        if (parts.length == 2 && parts[0].equals("k8s-api")) {
            return accessCluster(request, ApiContext.getContext().getIdFormatter().parseId(parts[1]));
        }

        if (parts.length == 2) {
            service = serviceDao.findServiceByName(ApiUtils.getPolicy().getAccountId(), parts[1], parts[0]);
        } else {
            service = serviceDao.findServiceByName(ApiUtils.getPolicy().getAccountId(), parts[0]);
        }

        if (service != null) {
            List<Long> instanceIds = DataAccessor.fieldLongList(service, ServiceConstants.FIELD_INSTANCE_IDS);
            if (instanceIds.size() > 0) {
                return actionHandler.perform(objectManager.loadResource(Instance.class, instanceIds.get(0)), request);
            }
        }

        request.setResponseCode(ResponseCodes.NOT_FOUND);
        return null;
    }

    private Cluster getCluster(String id) {
        Object obj = referenceValidator.getById(ClusterConstants.TYPE, id);
        if (obj instanceof Cluster) {
            return (Cluster) obj;
        }
        return null;
    }

    private Object accessCluster(ApiRequest request, String id) {
        Cluster cluster = getCluster(id);
        if (cluster == null) {
            return null;
        }

        K8sClientConfig clientConfig = DataAccessor.field(cluster, ClusterConstants.FIELD_K8S_CLIENT_CONFIG, K8sClientConfig.class);
        if (clientConfig == null || StringUtils.isBlank(clientConfig.getAddress())) {
            return null;
        }

        Instance instance = clusterDao.getAnyRancherAgent(cluster);
        if (instance == null) {
            return null;
        }

        Map<String, Object> data = CollectionUtils.asMap(
            "scheme",clientConfig.getAddress().endsWith("443") ? "https" : "http",
            "address", clientConfig.getAddress());

        Date expiration = new Date(System.currentTimeMillis() + ContainerProxyActionHandler.EXPIRE_SECONDS.get() * 1000);

        HostApiAccess apiAccess = apiService.getAccess(request, instance.getHostId(),
                CollectionUtils.asMap("proxy", data),
                expiration, ContainerProxyActionHandler.HOST_PROXY_PATH.get());

        if (apiAccess == null) {
            return null;
        }

        return new HostAccess(apiAccess.getUrl().replaceFirst("ws", "http"), apiAccess.getAuthenticationToken());
    }

}
