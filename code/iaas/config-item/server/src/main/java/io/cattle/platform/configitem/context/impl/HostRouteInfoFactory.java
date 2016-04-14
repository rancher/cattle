package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.context.dao.NetworkInfoDao;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.Request;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostRouteInfoFactory extends AbstractAgentBaseContextFactory {

    NetworkInfoDao networkInfo;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context, Request configRequest) {
        context.getData().put("instances", networkInfo.getHostInstances(agent));
        context.getData().put("routes", networkInfo.getHostRoutes(agent));
    }

    public NetworkInfoDao getNetworkInfo() {
        return networkInfo;
    }

    @Inject
    public void setNetworkInfo(NetworkInfoDao networkInfo) {
        this.networkInfo = networkInfo;
    }

}
