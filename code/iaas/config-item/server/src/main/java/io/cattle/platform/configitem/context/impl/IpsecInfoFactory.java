package io.cattle.platform.configitem.context.impl;

import static io.cattle.platform.core.model.tables.DataTable.*;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Data;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class IpsecInfoFactory extends AbstractAgentBaseContextFactory {

    NetworkDao networkDao;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        Network network = null;
        Data data = null;
        for (NetworkService service : networkDao.getAgentInstanceNetworkService(instance.getId(), NetworkServiceConstants.KIND_IPSEC_TUNNEL)) {
            if (network == null) {
                network = objectManager.loadResource(Network.class, service.getNetworkId());
                data = objectManager.findAny(Data.class, DATA.NAME, String.format("%s/%s", service.getUuid(), "ipsecKey"));
                break;
            }
        }

        if (data != null) {
            context.getData().put("ipsecNetwork", network);
            context.getData().put("ipsecKey", data.getValue());
        }
    }

    public NetworkDao getNetworkDao() {
        return networkDao;
    }

    @Inject
    public void setNetworkDao(NetworkDao networkDao) {
        this.networkDao = networkDao;
    }

}
