package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.context.dao.HealthcheckInfoDao;
import io.cattle.platform.configitem.context.data.HealthcheckData;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HealthcheckInfoFactory extends AbstractAgentBaseContextFactory {

    @Inject
    HealthcheckInfoDao healthCheckInfoDao;

    @Inject
    NetworkDao networkDao;

    @Inject
    IpAddressDao ipAddressDao;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        if (instance == null) {
            return;
        }
        Nic primaryNic = networkDao.getPrimaryNic(instance.getId());
        if (primaryNic == null) {
            return;
        }

        // ip address of the agent instance
        IpAddress ipAddress = ipAddressDao.getPrimaryIpAddress(primaryNic);
        if (ipAddress == null) {
            return;
        }

        // get healtcheck data for the instances
        List<HealthcheckData> healthCheckData = healthCheckInfoDao.getInstanceHealthcheckEntries(instance);
        healthCheckData.removeAll(Collections.singleton(null));

        context.getData().put("publicIp", ipAddress);
        context.getData().put("healthCheckEntries", healthCheckData);
    }

}
