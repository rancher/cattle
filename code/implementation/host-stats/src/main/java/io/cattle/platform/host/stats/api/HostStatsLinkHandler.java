package io.cattle.platform.host.stats.api;

import io.cattle.platform.api.link.LinkHandler;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.host.model.HostApiAccess;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.host.stats.utils.StatsConstants;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.token.TokenService;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class HostStatsLinkHandler implements LinkHandler {

    private static final String PROJECT_PATH = "/project/";

    @Inject
    HostApiService hostApiService;
    @Inject
    ObjectManager objectManager;
    @Inject
    TokenService tokenService;

    @Override
    public String[] getTypes() {
        return new String[] { HostConstants.TYPE, ProjectConstants.TYPE };
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
            HostApiAccess apiAccess = hostApiService.getAccess(request, host.getId(), payload, new String[] { StatsConstants.HOST_STATS_PATH.get() });
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
        Map<String, Object> metaQueryPayload = new HashMap<>();
        metaQueryPayload.put(ProjectConstants.TYPE, serviceStatsQuery);

        meta.setToken(tokenService.generateToken(metaQueryPayload));
        meta.setUrl(metaUrl);

        return meta;
    }

}