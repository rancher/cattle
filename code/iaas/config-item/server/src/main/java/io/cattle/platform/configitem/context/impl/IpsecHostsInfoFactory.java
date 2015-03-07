package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.context.dao.NetworkInfoDao;
import io.cattle.platform.configitem.context.data.ClientIpsecTunnelInfo;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class IpsecHostsInfoFactory extends AbstractAgentBaseContextFactory {

    NetworkInfoDao networkInfoDao;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        List<Host> hosts = objectManager.mappedChildren(instance, Host.class);

        if (hosts.size() > 0) {
            context.getData().put("currentHost", hosts.get(0));
            List<ClientIpsecTunnelInfo> clients = networkInfoDao.getIpsecTunnelClient(instance);

            for (ClientIpsecTunnelInfo client : clients) {
                if (client.getInstance().getId().equals(instance.getId())) {
                    context.getData().put("agentInstanceClient", client);
                }
            }

            context.getData().put("ipsecClients", clients);
        } else {
            context.getData().put("ipsecClients", Collections.emptyList());
        }
    }

    public NetworkInfoDao getNetworkInfoDao() {
        return networkInfoDao;
    }

    @Inject
    public void setNetworkInfoDao(NetworkInfoDao networkInfoDao) {
        this.networkInfoDao = networkInfoDao;
    }

}
