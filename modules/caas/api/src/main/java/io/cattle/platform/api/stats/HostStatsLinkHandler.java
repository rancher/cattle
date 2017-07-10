package io.cattle.platform.api.stats;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.hostapi.HostApiAccess;
import io.cattle.platform.hostapi.HostApiService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.token.TokenService;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.LinkHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HostStatsLinkHandler implements LinkHandler {

    private static final String PROJECT_PATH = "/project/";

    HostApiService hostApiService;
    ObjectManager objectManager;
    TokenService tokenService;

    public HostStatsLinkHandler(HostApiService hostApiService, ObjectManager objectManager, TokenService tokenService) {
        super();
        this.hostApiService = hostApiService;
        this.objectManager = objectManager;
        this.tokenService = tokenService;
    }

    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return StatsConstants.HOST_STATS.equalsIgnoreCase(link) && (ProjectConstants.TYPE.equalsIgnoreCase(type) || HostConstants.TYPE.equalsIgnoreCase(type));
    }

    @Override
    public Object link(String name, Object obj, ApiRequest request) throws IOException {
        List<Host> hosts = new ArrayList<>();
        boolean project = false;
        if (obj instanceof Host) {
            Host origHost = (Host) obj;
            hosts.add(origHost);
        }

        if (obj instanceof Account && AccountConstants.PROJECT_KIND.equals(((Account) obj).getKind())) {
            hosts.addAll(objectManager.mappedChildren(obj, Host.class));
            project = true;
        }

        if (hosts.size() == 0) {
            return new StatsAccess();
        }

        String metaUrl = null;
        StatsAccess meta = new StatsAccess();
        List<StatsAccess> serviceStatsQuery = new ArrayList<>();
        for (Host host : hosts) {
            Agent agent = objectManager.loadResource(Agent.class, host.getAgentId());
            if (agent == null || !CommonStatesConstants.ACTIVE.equals(agent.getState())) {
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("resourceId", ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(host), host.getId()));
            HostApiAccess apiAccess = hostApiService.getAccess(request, host.getId(), payload, StatsConstants.HOST_STATS_PATH.get());
            if (apiAccess == null) {
                continue;
            }

            String url = apiAccess.getUrl();

            metaUrl = url + PROJECT_PATH;

            StatsAccess statsAccess = new StatsAccess();
            statsAccess.setToken(apiAccess.getAuthenticationToken());
            statsAccess.setUrl(url.toString());

            if (!project) {
                return statsAccess;
            }

            serviceStatsQuery.add(statsAccess);
        }
        Map<String, Object> authToken = new HashMap<>();
        authToken.put("payload", true);

        Map<String, Object> metaQueryPayload = new HashMap<>();
        metaQueryPayload.put(ProjectConstants.TYPE, serviceStatsQuery);

        meta.setAuthToken(tokenService.generateToken(authToken));
        meta.setToken(tokenService.generateToken(metaQueryPayload));
        meta.setUrl(metaUrl);

        return meta;
    }

}