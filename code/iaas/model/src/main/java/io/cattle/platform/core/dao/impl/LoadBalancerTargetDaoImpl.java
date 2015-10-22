package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.LoadBalancerTargetTable.LOAD_BALANCER_TARGET;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.LoadBalancerTargetRecord;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class LoadBalancerTargetDaoImpl extends AbstractJooqDao implements LoadBalancerTargetDao {

    @Inject
    ObjectManager objectManager;

    @Inject
    LoadBalancerDao lbDao;

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Inject
    JsonMapper jsonMapper;

    @Override
    public LoadBalancerTarget getLoadBalancerTarget(long lbId, LoadBalancerTargetInput targetInput) {
        Map<Object, Object> criteria = new HashMap<>();
        criteria.put(LOAD_BALANCER_TARGET.LOAD_BALANCER_ID, lbId);
        criteria.put(LOAD_BALANCER_TARGET.IP_ADDRESS, targetInput.getIpAddress());
        criteria.put(LOAD_BALANCER_TARGET.INSTANCE_ID, targetInput.getInstanceId());
        criteria.put(LOAD_BALANCER_TARGET.REMOVED, null);
        return objectManager.findAny(LoadBalancerTarget.class, criteria);
    }

    @Override
    public List<? extends LoadBalancerTarget> listByLbIdToRemove(long lbId) {
        return create()
                .selectFrom(LOAD_BALANCER_TARGET)
                .where(
                        LOAD_BALANCER_TARGET.LOAD_BALANCER_ID.eq(lbId)
                                .and(LOAD_BALANCER_TARGET.REMOVED.isNull().
                                        or(LOAD_BALANCER_TARGET.STATE.eq(CommonStatesConstants.REMOVING))))
                .fetchInto(LoadBalancerTargetRecord.class);
    }

    @Override
    public List<? extends LoadBalancerTarget> listByLbId(long lbId) {
        return create()
                .selectFrom(LOAD_BALANCER_TARGET)
                .where(
                        LOAD_BALANCER_TARGET.LOAD_BALANCER_ID.eq(lbId)
                                .and(LOAD_BALANCER_TARGET.REMOVED.isNull()))
                .fetchInto(LoadBalancerTargetRecord.class);
    }

    @Override
    public List<? extends Instance> getLoadBalancerActiveTargetInstances(long lbId) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(LOAD_BALANCER_TARGET)
                .on(LOAD_BALANCER_TARGET.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(LOAD_BALANCER_TARGET.LOAD_BALANCER_ID.eq(lbId))
                        .and(LOAD_BALANCER_TARGET.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE)))
                .where(INSTANCE.REMOVED.isNull().and(
                        INSTANCE.STATE.in(InstanceConstants.STATE_RUNNING, InstanceConstants.STATE_STARTING,
                                InstanceConstants.STATE_RESTARTING)))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends LoadBalancerTarget> getLoadBalancerActiveIpTargets(long lbId) {
        return create()
                .select(LOAD_BALANCER_TARGET.fields())
                .from(LOAD_BALANCER_TARGET)
                .where(LOAD_BALANCER_TARGET.REMOVED.isNull()
                        .and(
                        LOAD_BALANCER_TARGET.STATE.in(CommonStatesConstants.ACTIVATING,
                                        CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE))
                        .and(LOAD_BALANCER_TARGET.IP_ADDRESS.isNotNull())
                        .and(LOAD_BALANCER_TARGET.LOAD_BALANCER_ID.eq(lbId)))
                .fetchInto(LoadBalancerTargetRecord.class);
    }

    @Override
    public List<? extends LoadBalancerTarget> getLoadBalancerActiveInstanceTargets(long lbId) {
        return create()
                .select(LOAD_BALANCER_TARGET.fields())
                .from(LOAD_BALANCER_TARGET)
                .where(LOAD_BALANCER_TARGET.REMOVED.isNull()
                        .and(
                                LOAD_BALANCER_TARGET.STATE.in(CommonStatesConstants.ACTIVATING,
                                        CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE))
                        .and(LOAD_BALANCER_TARGET.INSTANCE_ID.isNotNull())
                        .and(LOAD_BALANCER_TARGET.LOAD_BALANCER_ID.eq(lbId)))
                .fetchInto(LoadBalancerTargetRecord.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LoadBalancerTargetPortSpec> getLoadBalancerTargetPorts(LoadBalancerTarget target, LoadBalancerConfig config) {
        List<LoadBalancerTargetPortSpec> portSpecsInitial = new ArrayList<>();
        List<? extends LoadBalancerListener> listeners = lbDao.listActiveListenersForConfig(config.getId());
        Map<Integer, LoadBalancerListener> lbSourcePorts = new HashMap<>();
        for (LoadBalancerListener listener : listeners) {
            lbSourcePorts.put(getSourcePort(listener), listener);
        }

        List<Integer> targetSourcePorts = new ArrayList<>();

        List<String> portsData = DataAccessor.fields(target)
                .withKey(LoadBalancerConstants.FIELD_LB_TARGET_PORTS).withDefault(Collections.EMPTY_LIST)
                .as(List.class);
        if (portsData != null && !portsData.isEmpty()) {
            for (String portData : portsData) {
                portSpecsInitial.add(new LoadBalancerTargetPortSpec(portData));
            }
        }

        List<LoadBalancerTargetPortSpec> portSpecsToReturn = completePortSpecs(portSpecsInitial, listeners,
                lbSourcePorts, targetSourcePorts);

        addMissingPortSpecs(lbSourcePorts, targetSourcePorts, portSpecsToReturn);

        return portSpecsToReturn;
    }

    protected void addMissingPortSpecs(Map<Integer, LoadBalancerListener> lbSourcePorts,
            List<Integer> targetSourcePorts, List<LoadBalancerTargetPortSpec> completePortSpecs) {
        // create port specs for missing load balancer source ports
        for (Integer lbSourcePort : lbSourcePorts.keySet()) {
            if (!targetSourcePorts.contains(lbSourcePort)) {
                LoadBalancerListener listener = lbSourcePorts.get(lbSourcePort);
                completePortSpecs
                        .add(new LoadBalancerTargetPortSpec(listener.getTargetPort(), getSourcePort(listener)));
            }
        }
    }

    protected Integer getSourcePort(LoadBalancerListener listener) {
        // LEGACY code to support the case when private port is not defined
        return listener.getPrivatePort() != null ? listener.getPrivatePort() : listener.getSourcePort();
    }

    protected List<LoadBalancerTargetPortSpec> completePortSpecs(List<LoadBalancerTargetPortSpec> portSpecsInitial,
            List<? extends LoadBalancerListener> listeners, Map<Integer, LoadBalancerListener> lbSourcePorts,
            List<Integer> targetSourcePorts) {
        // complete missing source ports for port specs
        List<LoadBalancerTargetPortSpec> portSpecsWithSourcePorts = new ArrayList<>();
        for (LoadBalancerTargetPortSpec portSpec : portSpecsInitial) {
            if (portSpec.getSourcePort() == null) {
                for (LoadBalancerListener listener : listeners) {
                    LoadBalancerTargetPortSpec newSpec = new LoadBalancerTargetPortSpec(portSpec);
                    newSpec.setSourcePort(getSourcePort(listener));
                    portSpecsWithSourcePorts.add(newSpec);
                    // register the fact that the source port is defined on the target
                    targetSourcePorts.add(newSpec.getSourcePort());
                }
            } else {
                portSpecsWithSourcePorts.add(portSpec);
                // register the fact that the source port is defined on the target
                targetSourcePorts.add(portSpec.getSourcePort());
            }
        }
        
        // complete missing target ports
        List<LoadBalancerTargetPortSpec> completePortSpecs = new ArrayList<>();
        for (LoadBalancerTargetPortSpec spec : portSpecsWithSourcePorts) {
            if (spec.getPort() == null) {
                LoadBalancerListener listener = lbSourcePorts.get(spec.getSourcePort());
                if (listener != null) {
                    spec.setPort(listener.getTargetPort());
                    completePortSpecs.add(spec);
                }
            } else {
                completePortSpecs.add(spec);
            }
        }
        return completePortSpecs;
    }

    @Override
    public void createLoadBalancerTarget(LoadBalancer lb, LoadBalancerTargetInput toAdd) {
        LoadBalancerTarget target = getLoadBalancerTarget(lb.getId(), toAdd);
        if (target == null) {
            target = resourceDao.createAndSchedule(LoadBalancerTarget.class, LOAD_BALANCER_TARGET.INSTANCE_ID, null,
                    LOAD_BALANCER_TARGET.LOAD_BALANCER_ID, lb.getId(),
                    LOAD_BALANCER_TARGET.IP_ADDRESS, toAdd.getIpAddress(),
                    LOAD_BALANCER_TARGET.INSTANCE_ID, toAdd.getInstanceId(),
                    LOAD_BALANCER_TARGET.ACCOUNT_ID, lb.getAccountId(),
                    LoadBalancerConstants.FIELD_LB_TARGET_PORTS, toAdd.getPorts());
        } else {
            List<? extends String> newPorts = toAdd.getPorts() != null ? toAdd.getPorts()
                    : new ArrayList<String>();
            List<? extends String> existingPorts = DataAccessor.fields(target).withKey(LoadBalancerConstants.FIELD_LB_TARGET_PORTS).
                    withDefault(Collections.EMPTY_LIST).asList(jsonMapper, String.class);
            if (!newPorts.containsAll(existingPorts) || !existingPorts.contains(newPorts)) {
                DataUtils.getWritableFields(target).put(LoadBalancerConstants.FIELD_LB_TARGET_PORTS, newPorts);
                objectManager.persist(target);
                objectProcessManager.scheduleStandardProcess(StandardProcess.UPDATE, target, null);
            }
        }
    }

    @Override
    public void removeLoadBalancerTarget(LoadBalancer lb, LoadBalancerTargetInput toRemove) {
        LoadBalancerTarget target = getLoadBalancerTarget(lb.getId(), toRemove);

        if (target != null) {
            objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_TARGET_MAP_REMOVE,
                    target, null);
        }
    }
}
