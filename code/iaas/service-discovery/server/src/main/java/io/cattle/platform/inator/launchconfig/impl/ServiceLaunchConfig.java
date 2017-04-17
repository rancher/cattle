package io.cattle.platform.inator.launchconfig.impl;


import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.InstanceHealthCheck.Strategy;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.Services;
import io.cattle.platform.inator.launchconfig.LaunchConfig;
import io.cattle.platform.inator.unit.InstanceUnit;
import io.cattle.platform.inator.unit.MissingUnit;
import io.cattle.platform.inator.util.InstanceFactory;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.InstanceWrapper;
import io.cattle.platform.inator.wrapper.ServiceRevisionWrapper;
import io.cattle.platform.inator.wrapper.StackWrapper;
import io.cattle.platform.network.IPAssignment;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class ServiceLaunchConfig implements LaunchConfig {

    String name;
    Map<String, Object> lc;
    InstanceHealthCheck healthCheck;
    ServiceRevisionWrapper service;
    Services svc;

    public ServiceLaunchConfig(String name, Map<String, Object> lc, InstanceHealthCheck healthCheck, ServiceRevisionWrapper service,
            Services svc) {
        super();
        this.name = name;
        this.lc = lc;
        this.healthCheck = healthCheck;
        this.service = service;
        this.svc = svc;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<UnitRef, Unit> getDependenciesHolders() {
        Map<UnitRef, Unit> deps = new HashMap<>();

        getDependencyRefsByField().forEach((k, v) -> {
            v.refs().forEach((ref) -> deps.put(ref, new MissingUnit(ref)));
        });

        return deps;
    }

    protected Map<String, UnitRefOrListUnitRef> getDependencyRefsByField() {
        Map<String, UnitRefOrListUnitRef> result = new HashMap<>();
        for (String dep : ServiceConstants.NS_DEPS) {
            Object obj = DataAccessor.fromMap(lc).withKey(dep).get();
            if (obj == null || StringUtils.isBlank(obj.toString())) {
                continue;
            }

            UnitRef ref = null;
            List<UnitRef> refs = null;
            if (obj instanceof List<?>) {
                refs = ((List<?>)obj).stream()
                    .map((o) -> getUnitRefForDep(o.toString()))
                    .collect(Collectors.toList());
            } else if (obj instanceof String) {
                ref = getUnitRefForDep(obj.toString());
            }

            result.put(dep, new UnitRefOrListUnitRef(ref, refs));
        }

        return result;
    }

    protected UnitRef getUnitRefForDep(String otherLcName) {
        if (StringUtils.isBlank(otherLcName)) {
            return null;
        }

        LaunchConfig otherLc = service.getLaunchConfigs().get(otherLcName);
        String revision = "";
        if (otherLc != null) {
            revision = otherLc.getRevision();
        }
        return new UnitRef(revision + "/" + otherLcName);
    }

    @Override
    public InstanceWrapper create(InatorContext context, StackWrapper stack, DeploymentUnitWrapper unit) {
        ServiceIndex serviceIndex = createServiceIndex(service, unit.getServiceIndex());
        Instance instance = createInstance(stack, service, unit, context, serviceIndex);
        return new InstanceWrapper(instance, this, svc);
    }

    protected ServiceIndex createServiceIndex(ServiceRevisionWrapper service, int index) {
        ServiceIndex serviceIndex = svc.serviceDao.createServiceIndex(service.getServiceId(), name, index);

        // allocate ip address if not set
        if (service.isRetainIP()) {
            String requestedIP = getRequestedIP();
            allocateIpToServiceIndex(serviceIndex, requestedIP);
        }

        return serviceIndex;
    }

    public void allocateIpToServiceIndex(ServiceIndex serviceIndex, String requestedIp) {
        String networkMode = svc.networkService.getNetworkMode(lc);
        if (StringUtils.isBlank(networkMode) || !StringUtils.isEmpty(serviceIndex.getAddress())) {
            return;
        }

        Network ntwk = svc.networkService.resolveNetwork(serviceIndex.getAccountId(), networkMode.toString());
        if (svc.networkService.shouldAssignIpAddress(ntwk)) {
            IPAssignment assignment = svc.networkService.assignIpAddress(ntwk, serviceIndex, requestedIp);
            if (assignment != null) {
                svc.objectManager.setFields(serviceIndex, IpAddressConstants.FIELD_ADDRESS, assignment.getIpAddress());
            }
        }
    }

    protected String getRequestedIP() {
        String requestedIP = DataAccessor.fromMap(lc)
                .withKey(InstanceConstants.FIELD_REQUESTED_IP_ADDRESS)
                .as(String.class);

        if (!StringUtils.isBlank(requestedIP)) {
            return requestedIP;
        }

        // can be passed via labels
        Map<String, Object> labels = CollectionUtils.toMap(DataAccessor.fromMap(lc)
                .withKey(InstanceConstants.FIELD_LABELS)
                .get());
        return DataAccessor.fromMap(labels).withKey(SystemLabels.LABEL_REQUESTED_IP).as(String.class);
    }

    protected Instance createInstance(StackWrapper stack, ServiceRevisionWrapper service,
            DeploymentUnitWrapper unit, InatorContext context, ServiceIndex index) {
        Map<String, Object> instanceDependencies = bindDependencies(context);
        String instanceName = getInstanceName(stack, service, index.getServiceIndex());

        Map<String, Object> instanceData = InstanceFactory.createInstanceData(lc,
                stack,
                service,
                unit,
                name,
                index,
                instanceName);
        instanceData.putAll(instanceDependencies);

        Pair<Instance, ServiceExposeMap> pair =  svc.serviceDao.createServiceInstance(instanceData, service.getServiceId());
        return pair.getLeft();
    }

    protected Map<String, Object> bindDependencies(InatorContext context) {
        Map<UnitRef, Unit> units = context.getUnits();
        Map<String, Object> bound = new HashMap<>();
        getDependencyRefsByField().forEach((fieldName, refs) -> {
            Object ids = bindUnitRefOrListUnitRefToInstanceIds(units, refs);
            if (ids != null) {
                bound.put(fieldName, ids);
            }
        });
        return bound;
    }

    protected Object bindUnitRefOrListUnitRefToInstanceIds(Map<UnitRef, Unit> units, UnitRefOrListUnitRef refOrList) {
        List<Object> ids = new ArrayList<>(refOrList.refs().size());

        refOrList.refs().forEach((ref) -> {
            Unit unit = units.get(ref);
            if (unit instanceof InstanceUnit) {
                ids.add(((InstanceUnit)unit).getInstanceId());
            }
        });

        if (refOrList.isSingle()) {
            return ids.size() == 0 ? null : ids.get(0);
        }

        return ids.size() > 0 ? ids : null;
    }

    public String getInstanceName(StackWrapper stack, ServiceRevisionWrapper service, String index) {
        return ServiceUtil.generateServiceInstanceName(stack.getName(), service.getName(), name, index);
    }

    @Override
    public String getRevision() {
        return DataAccessor.fromMap(lc).withKey(ServiceConstants.FIELD_VERSION).withDefault("").as(String.class);
    }

    @Override
    public boolean hasQuorum() {
        if (healthCheck == null) {
            return false;
        }
        return healthCheck.getStrategy() == Strategy.recreateOnQuorum;
    }

    @Override
    public boolean isStartFirst() {
        return service.isStartFirst();
    }

    private static class UnitRefOrListUnitRef {
        UnitRef ref = null;
        List<UnitRef> refList = null;

        public UnitRefOrListUnitRef(UnitRef ref, List<UnitRef> refList) {
            super();
            this.ref = ref;
            this.refList = refList;
        }

        public boolean isSingle() {
            return this.ref != null;
        }

        public List<UnitRef> refs() {
            if (ref != null) {
                return Arrays.asList(ref);
            }
            return refList;
        }
    }

}