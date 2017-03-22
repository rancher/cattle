package io.cattle.platform.host.stats.api;

import io.cattle.platform.api.link.LinkHandler;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.dao.StackDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.docker.util.DockerUtils;
import io.cattle.platform.host.model.HostApiAccess;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.host.stats.utils.StatsConstants;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class ServiceContainerStatsLinkHandler implements LinkHandler {

    @Inject
    HostApiService hostApiService;
    @Inject
    ObjectManager objectManager;
    @Inject
    TokenService tokenService;
    @Inject
    ServiceExposeMapDao exposeDao;
    @Inject
    StackDao stackDao;

    private static final String SERVICE_PATH = "/service/";

    @Override
    public String[] getTypes() {
        return new String[] { "service", "loadBalancerService", "stack" };
    }

    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return StatsConstants.CONTAINER_STATS.equalsIgnoreCase(link);
    }

    @Override
    public Object link(String name, Object obj, ApiRequest request) throws IOException {
        IdFormatter idF = ApiContext.getContext().getIdFormatter();
        List<? extends Service> services = getServices(obj);

        if (services.isEmpty()) {
            return null;
        }

        StatsAccess meta = new StatsAccess();
        List<StatsAccess> serviceStatsQuery = new ArrayList<>();
        Map<Long, Map<String, Object>> containersByHost = new HashMap<>();

        for (Service service : services) {
            for (Instance instance : exposeDao.listServiceManagedInstances(service)) {
                Long hostId = DataAccessor.fieldLong(instance, InstanceConstants.FIELD_HOST_ID);
                if (hostId == null) {
                    continue;
                }
                Map<String, Object> containerIdsMap = containersByHost.get(hostId);
                if (containerIdsMap == null) {
                    containerIdsMap = new HashMap<>();
                    containersByHost.put(hostId, containerIdsMap);
                }

                containerIdsMap.put(DockerUtils.getDockerIdentifier(instance),
                        idF.formatId(objectManager.getType(instance), instance.getId()));
            }
        }

        containersByHost.forEach((hostId, containerIdsMap) -> {
            Map<String, Object> payload = CollectionUtils.asMap("containerIds", containerIdsMap);
            HostApiAccess apiAccess = hostApiService.getAccess(request, hostId, payload,
                    StatsConstants.CONTAINER_STATS_PATH.get());
            if (apiAccess == null) {
                return;
            }

            meta.setUrl(apiAccess.getUrl() + SERVICE_PATH);

            StatsAccess statsAccess = new StatsAccess();
            statsAccess.setToken(apiAccess.getAuthenticationToken());
            statsAccess.setUrl(apiAccess.getUrl().toString());
            serviceStatsQuery.add(statsAccess);
        });


        Map<String, Object> authToken = new HashMap<>();
        authToken.put("payload", true);

        Map<String, Object> metaQueryPayload = new HashMap<>();
        metaQueryPayload.put("service", serviceStatsQuery);

        meta.setAuthToken(tokenService.generateToken(authToken));
        meta.setToken(tokenService.generateToken(metaQueryPayload));

        return meta;
    }

    protected List<? extends Service> getServices(Object obj) {
        if (obj instanceof Service) {
            return Arrays.asList((Service)obj);
        } else if (obj instanceof Stack) {
            return stackDao.getServices(((Stack) obj).getId());
        }

        return Collections.emptyList();
    }

}