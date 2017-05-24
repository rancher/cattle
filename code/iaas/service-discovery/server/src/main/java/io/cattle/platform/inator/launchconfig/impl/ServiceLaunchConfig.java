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
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.InstanceBindable;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.launchconfig.LaunchConfig;
import io.cattle.platform.inator.unit.InstanceUnit;
import io.cattle.platform.inator.unit.MissingUnit;
import io.cattle.platform.inator.util.InatorUtils;
import io.cattle.platform.inator.util.InstanceFactory;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.InstanceWrapper;
import io.cattle.platform.inator.wrapper.RevisionWrapper;
import io.cattle.platform.inator.wrapper.StackWrapper;
import io.cattle.platform.network.IPAssignment;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class ServiceLaunchConfig implements LaunchConfig {

    String name;
    Map<String, Object> lc;
    Map<String, VolumeTemplate> vts;
    InstanceHealthCheck healthCheck;
    RevisionWrapper currentService;
    InatorServices svc;
    DataVolumes dataVolumes;
    RandomPorts ports;
    Map<UnitRef, Unit> dependencies;

    public ServiceLaunchConfig(String name, Map<String, Object> lc, InstanceHealthCheck healthCheck, RevisionWrapper currentService,
            Map<String, VolumeTemplate> vts, boolean serviceManaged, InatorServices svc) {
        super();
        this.name = name;
        this.lc = lc;
        this.healthCheck = healthCheck;
        // Note this isn't the servicerevision that holds this LC, but the current to be applied service revision
        this.currentService = currentService;
        this.svc = svc;
        this.vts = vts;
        this.dataVolumes = new DataVolumes(lc, svc);
        if (serviceManaged) {
            this.ports = new RandomPorts(name, lc, svc);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<UnitRef, Unit> getDependencies() {
        if (dependencies != null) {
            return dependencies;
        }

        Map<UnitRef, Unit> deps = new HashMap<>();

        // Namespace dependencies, and volumeFrom
        getDependencyRefsByField().forEach((k, v) -> {
            v.refs().forEach((ref) -> deps.put(ref, new MissingUnit(ref)));
        });

        // Volumes from volumeTempltes
        deps.putAll(this.dataVolumes.getVolumes(vts));

        if (this.ports != null) {
            deps.putAll(this.ports.getPorts());
        }

        return dependencies = deps;
    }

    protected Map<String, UnitRefList> getDependencyRefsByField() {
        Map<String, UnitRefList> result = new HashMap<>();
        for (String dep : ServiceConstants.NS_DEPS) {
            Object obj = DataAccessor.fromMap(lc).withKey(dep).get();
            if (obj == null || StringUtils.isBlank(obj.toString())) {
                continue;
            }

            List<UnitRef> refs = CollectionUtils.toList(obj).stream()
                    .map((o) -> getUnitRefForDep(o.toString()))
                    .collect(Collectors.toList());

            result.put(ServiceConstants.NS_DEP_FIELD_MAPPING.get(dep),
                    new UnitRefList(refs, !(obj instanceof List<?>)));
        }

        return result;
    }

    protected UnitRef getUnitRefForDep(String otherLcName) {
        if (StringUtils.isBlank(otherLcName)) {
            return null;
        }

        LaunchConfig otherLc = currentService.getLaunchConfig(otherLcName);
        String revision = "";
        if (otherLc != null) {
            revision = otherLc.getRevision();
            // Name may change to primay launch config
            otherLcName = otherLc.getName();
        }
        return new UnitRef(String.format("instance/%s/%s", revision, otherLcName));
    }

    @Override
    public InstanceWrapper create(InatorContext context, StackWrapper stack, DeploymentUnitWrapper unit) {
        ServiceIndex serviceIndex = createServiceIndex(currentService, unit.getServiceIndex());
        Pair<Instance, ServiceExposeMap> pair = createInstance(stack, currentService, unit, context, serviceIndex);
        return new InstanceWrapper(pair.getLeft(), pair.getRight(), serviceIndex, svc);
    }

    @Override
    public boolean validateDeps(InatorContext context, InstanceWrapper instanceWrapper) {
        Instance instance = instanceWrapper.getInternal();
        Map<String, Object> instanceDependencies = new HashMap<>();
        bindDependencies(context, instanceDependencies);

        for (String hardDep : ServiceConstants.HARD_DEPS) {
            Object value = instanceDependencies.get(hardDep);
            Object existingValue = DataAccessor.field(instance, hardDep, Object.class);
            if (existingValue == null) {
                existingValue = io.cattle.platform.object.util.ObjectUtils.getPropertyIgnoreErrors(instance, hardDep);
            }
            if (!InatorUtils.objAndNumListEquals(value, existingValue)) {
                return false;
            }
        }

        Map<String, Object> dataVolumesMounts = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_DATA_VOLUME_MOUNTS);
        for (Object idObj : dataVolumesMounts.values()) {
            if (idObj instanceof Long) {
                Volume volume = svc.objectManager.loadResource(Volume.class, (Long) idObj);
                if (volume != null && volume.getRemoved() != null) {
                    return false;
                }
            }
        }

        return true;
    }

    protected ServiceIndex createServiceIndex(RevisionWrapper service, String index) {
        ServiceIndex serviceIndex = svc.serviceDao.createServiceIndex(service.getServiceId(), name, index);

        // allocate ip address if not set
        if (service.isRetainIP()) {
            String requestedIP = getRequestedIP();
            allocateIpToServiceIndex(service.getAccountId(), serviceIndex, requestedIP);
        }

        return serviceIndex;
    }

    protected void allocateIpToServiceIndex(long accountId, ServiceIndex serviceIndex, String requestedIp) {
        String networkMode = svc.networkService.getNetworkMode(lc);
        if (StringUtils.isBlank(networkMode) || !StringUtils.isEmpty(serviceIndex.getAddress())) {
            return;
        }

        Network ntwk = svc.networkService.resolveNetwork(accountId, networkMode.toString());
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

    protected Pair<Instance, ServiceExposeMap> createInstance(StackWrapper stack, RevisionWrapper service,
            DeploymentUnitWrapper unit, InatorContext context, ServiceIndex index) {
        String instanceName = getInstanceName(stack, service, index);

        Map<String, Object> instanceData = InstanceFactory.createInstanceData(lc,
                stack,
                service,
                unit,
                name,
                index,
                instanceName);

        bindDependencies(context, instanceData);

        Long next = svc.serviceDao.getNextCreate(service.getServiceId());
        Pair<Instance, ServiceExposeMap> pair =  svc.serviceDao.createServiceInstance(instanceData, service.getServiceId(), next);
        return pair;
    }

    protected void bindDependencies(InatorContext context, Map<String, Object> instanceData) {
        Map<UnitRef, Unit> units = context.getUnits();

        getDependencyRefsByField().forEach((fieldName, refs) -> {
            Object ids = convertUnitRefsToInstanceIds(units, refs);
            if (ids != null) {
                instanceData.put(fieldName, ids);
            }
        });

        for (UnitRef ref : getDependencies().keySet()) {
            Unit unit = units.get(ref);
            if (unit instanceof InstanceBindable) {
                ((InstanceBindable) unit).bind(context, instanceData);
            }
        };
    }

    @Override
    public void applyDynamic(InstanceWrapper instance, InatorContext context) {
        if (this.ports != null) {
            processDynamicPorts(instance, context);
        }
    }

    protected void processDynamicPorts(InstanceWrapper instance, InatorContext context) {
        Map<String, Object> testData = new HashMap<>();
        testData.put(InstanceConstants.FIELD_PORTS, lc.get(InstanceConstants.FIELD_PORTS));

        this.ports.getPorts().forEach((ref, unit) -> {
            if (unit instanceof InstanceBindable) {
                ((InstanceBindable) unit).bind(context, testData);
            }
        });

        Set<String> currentPorts = instance.getPorts().stream().collect(Collectors.toSet());
        List<String> newPortList = CollectionUtils.toList(testData.get(InstanceConstants.FIELD_PORTS)).stream()
                    .map((x) -> x.toString())
                    .collect(Collectors.toList());
        Set<String> newPorts = newPortList.stream().collect(Collectors.toSet());

        if (currentPorts.equals(newPorts)) {
            return;
        }

        svc.instanceDao.updatePorts(instance.getInternal(), newPortList);
    }

    protected Object convertUnitRefsToInstanceIds(Map<UnitRef, Unit> units, UnitRefList refOrList) {
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

    public String getInstanceName(StackWrapper stack, RevisionWrapper service, ServiceIndex index) {
        if (index == null) {
            return name;
        }
        return ServiceUtil.generateServiceInstanceName(stack.getName(), service.getName(), name, index.getServiceIndex());
    }

    @Override
    public String getRevision() {
        return DataAccessor.fromMap(lc).withKey(ServiceConstants.FIELD_VERSION).withDefault("0").as(String.class);
    }

    @Override
    public boolean isStartFirst() {
        return currentService.isStartFirst();
    }

    private static class UnitRefList{
        List<UnitRef> refList = null;
        boolean single;

        public UnitRefList(List<UnitRef> refList, boolean single) {
            this.refList = refList;
            this.single = single;
        }

        public boolean isSingle() {
            return single;
        }

        public List<UnitRef> refs() {
            return refList;
        }
    }

    @Override
    public boolean isHealthcheckActionNone() {
        if (healthCheck == null) {
            return false;
        }
        return healthCheck.getStrategy() == Strategy.none;
    }

    @Override
    public String getImageUuid() {
        return ObjectUtils.toString(lc.get(InstanceConstants.FIELD_IMAGE_UUID));
    }

    @Override
    public Map<String, Object> getLabels() {
        return CollectionUtils.toMap(lc.get(InstanceConstants.FIELD_LABELS));
    }

    @Override
    public String getPullMode() {
        String mode = ObjectUtils.toString(lc.get(InstanceConstants.FIELD_IMAGE_PRE_PULL));
        return mode == null ? InstanceConstants.PULL_NONE : mode;
    }

}