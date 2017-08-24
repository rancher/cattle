package io.cattle.platform.api.stats;

import io.cattle.platform.core.dao.StackDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.docker.util.DockerUtils;
import io.cattle.platform.hostapi.HostApiAccess;
import io.cattle.platform.hostapi.HostApiService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.LinkHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.cattle.platform.core.model.tables.InstanceTable.*;

public class ServiceContainerStatsLinkHandler implements LinkHandler {

    private static final String SERVICE_PATH = "/service/";

    HostApiService hostApiService;
    ObjectManager objectManager;
    TokenService tokenService;
    StackDao stackDao;

    public ServiceContainerStatsLinkHandler(HostApiService hostApiService, ObjectManager objectManager, TokenService tokenService,
            StackDao stackDao) {
        super();
        this.hostApiService = hostApiService;
        this.objectManager = objectManager;
        this.tokenService = tokenService;
        this.stackDao = stackDao;
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
            List<Instance> instances = objectManager.find(Instance.class,
                    INSTANCE.SERVICE_ID, service.getId(),
                    INSTANCE.REMOVED, null);

            for (Instance instance : instances) {
                Long hostId = instance.getHostId();
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
            statsAccess.setUrl(apiAccess.getUrl());
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
