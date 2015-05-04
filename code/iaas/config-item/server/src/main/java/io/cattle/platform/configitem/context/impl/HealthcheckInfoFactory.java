package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.context.dao.HealthcheckInfoDao;
import io.cattle.platform.configitem.context.data.HealthcheckData;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Inject
    JsonMapper jsonMapper;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        List<HealthcheckData> healthCheckData = healthCheckInfoDao.getHealthcheckEntries(instance);
        Nic primaryNic = networkDao.getPrimaryNic(instance.getId());
        if (primaryNic == null) {
            return;
        }
        IpAddress ipAddress = ipAddressDao.getPrimaryIpAddress(primaryNic);
        if (ipAddress == null) {
            return;
        }
        healthCheckData.removeAll(Collections.singleton(null));

        // group per healtcheck before passing to ftl file
        Map<Long, InstanceHealthCheck> serviceHealthCheckMap = new HashMap<>();
        Map<Long, List<IpAddress>> serviceIpsMap = new HashMap<>();
        List<HealthCheckEntry> healthCheckEntries = new ArrayList<>();
        for (HealthcheckData data : healthCheckData) {
            Service service = data.getService();
            // get healthcheck
            InstanceHealthCheck healthCheck = DataAccessor.field(service,
                    LoadBalancerConstants.FIELD_LB_HEALTH_CHECK, jsonMapper, InstanceHealthCheck.class);

            if (healthCheck == null) {
                continue;
            }
            if (serviceHealthCheckMap.get(service.getId()) == null) {
                serviceHealthCheckMap.put(service.getId(), healthCheck);
            }

            if (serviceIpsMap.get(service.getId()) == null) {
                serviceIpsMap.put(service.getId(), new ArrayList<IpAddress>());
            }
            serviceIpsMap.get(service.getId()).add(data.getTargetIpAddress());
        }

        for (Long serviceId : serviceHealthCheckMap.keySet()) {
            if (!serviceIpsMap.get(serviceId).isEmpty()) {
                healthCheckEntries.add(new HealthCheckEntry(serviceHealthCheckMap.get(serviceId), serviceIpsMap
                        .get(serviceId)));
            }
        }

        context.getData().put("publicIp", ipAddress);
        context.getData().put("healthCheckEntries", healthCheckEntries);
    }
    
    public class HealthCheckEntry {
        public InstanceHealthCheck healthCheck;
        public List<IpAddress> targetIpAddresses = new ArrayList<>();

        public HealthCheckEntry(InstanceHealthCheck healthCheck, List<IpAddress> targetIpAddresses) {
            this.healthCheck = healthCheck;
            this.targetIpAddresses = targetIpAddresses;
        }

        public InstanceHealthCheck getHealthCheck() {
            return healthCheck;
        }

        public void setHealthCheck(InstanceHealthCheck healthCheck) {
            this.healthCheck = healthCheck;
        }

        public List<IpAddress> getTargetIpAddresses() {
            return targetIpAddresses;
        }

        public void setTargetIpAddresses(List<IpAddress> targetIpAddresses) {
            this.targetIpAddresses = targetIpAddresses;
        }
    }

}
