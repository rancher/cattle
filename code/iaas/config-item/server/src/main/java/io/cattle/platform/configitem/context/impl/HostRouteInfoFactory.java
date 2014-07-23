package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.context.dao.NetworkInfoDao;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkService;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostRouteInfoFactory extends AbstractAgentBaseContextFactory {

    NetworkInfoDao networkInfo;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        boolean found = false;
        for ( NetworkService service : networkInfo.networkServices(agent) ) {
            if ( NetworkServiceConstants.KIND_HOST_NAT_GATEWAY.equals(service.getKind()) ) {
                found = true;
                break;
            }
        }

        if ( found ) {
            context.getData().put("ipAssociations", networkInfo.getHostIps(agent));
        }
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
