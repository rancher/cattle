package io.cattle.platform.inator.launchconfig.impl;


import io.cattle.platform.core.addon.DependsOn;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.InstanceHealthCheck.Strategy;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.InstanceBindable;
import io.cattle.platform.inator.Result;
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
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Override
    public List<DependsOn> getDependsOn() {
        Object obj = DataAccessor.fromMap(lc).withKey(InstanceConstants.FIELD_DEPENDS_ON).get();
        if (obj == null) {
            return Collections.emptyList();
        }

        return svc.jsonMapper.convertCollectionValue(obj, List.class, DependsOn.class);
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

            result.put(dep, new UnitRefList(refs, !(obj instanceof List<?>)));
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
        Instance instance = createInstance(stack, currentService, unit, context, unit.getServiceIndex());
        return new InstanceWrapper(instance, svc);
    }

    @Override
    public boolean validateDeps(InatorContext context, InstanceWrapper instanceWrapper) {
        Instance instance = instanceWrapper.getInternal();
        Map<String, Object> instanceDependencies = new HashMap<>();
        bindDependencies(context, instanceDependencies);

        for (String hardDep : ServiceConstants.NS_DEPS) {
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
            if (idObj instanceof Number) {
                Volume volume = svc.objectManager.loadResource(Volume.class, ((Number) idObj).longValue());
                if (volume != null && volume.getRemoved() != null) {
                    return false;
                }
            }
        }

        return true;
    }

    protected Instance createInstance(StackWrapper stack, RevisionWrapper service,
            DeploymentUnitWrapper unit, InatorContext context, int index) {
        String instanceName = getInstanceName(stack, service, index);

        Map<String, Object> instanceData = InstanceFactory.createInstanceData(lc,
                stack,
                service,
                unit,
                name,
                instanceName);

        bindDependencies(context, instanceData);

        Long next = svc.serviceDao.getNextCreate(service.getServiceId());
        return svc.serviceDao.createServiceInstance(instanceData, service.getServiceId(), next);
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
        }
    }

    @Override
    public Result applyDynamic(InstanceWrapper instance, InatorContext context) {
        if (this.ports != null) {
            return processDynamicPorts(instance, context);
        }
        return Result.good();
    }

    @Override
    public String getServiceName() {
        return name.equals(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME) ? currentService.getName() : name;
    }

    protected Result processDynamicPorts(InstanceWrapper instance, InatorContext context) {
        Map<String, Object> testData = new HashMap<>();
        testData.put(InstanceConstants.FIELD_PORTS, lc.get(InstanceConstants.FIELD_PORTS));

        this.ports.getPorts().forEach((ref, unit) -> {
            if (unit instanceof InstanceBindable) {
                ((InstanceBindable) unit).bind(context, testData);
            }
        });

        Set<String> currentPorts = new HashSet<>(instance.getPorts());
        List<String> newPortList = CollectionUtils.toList(testData.get(InstanceConstants.FIELD_PORTS)).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        Set<String> newPorts = new HashSet<>(newPortList);

        if (currentPorts.equals(newPorts)) {
            return Result.good();
        }

        boolean removed[] = new boolean[]{false};
        svc.metadataManager.getMetadataForAccount(instance.getInternal().getAccountId()).modify(Instance.class, instance.getId(), (i) -> {
            DataAccessor.setField(i, InstanceConstants.FIELD_PORTS, new ArrayList<>(newPorts));

            if (InstanceConstants.STATE_RUNNING.equals(i.getState())) {
                if (!svc.portManager.optionallyAssignPorts(i.getClusterId(), i.getHostId(), i.getId(), PortSpec.getPorts(i))) {
                    removed[0] = true;
                    svc.processManager.stopThenRemove(i, null);
                    return i;
                }
            }

            svc.objectManager.persist(i);
            return i;
        });

        if (removed[0]) {
            return new Result(Unit.UnitState.WAITING, null, "Recreating instance due to port scheduling");
        }

        return Result.good();
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

    public String getInstanceName(StackWrapper stack, RevisionWrapper service, int index) {
        if (index <= 0) {
            return service.getName();
        }
        return ServiceUtil.generateServiceInstanceName(stack.getName(), service.getName(), name, index);
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
        return ObjectUtils.toString(lc.get(InstanceConstants.FIELD_IMAGE));
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