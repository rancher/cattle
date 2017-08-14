package io.cattle.platform.process.externalevent;

import io.cattle.platform.allocator.constraint.HostAffinityConstraint;
import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ExternalEventConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.dao.StackDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.ExternalEvent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.cattle.platform.core.constants.ExternalEventConstants.*;
import static io.cattle.platform.core.model.Tables.*;
import static io.cattle.platform.core.model.tables.AgentTable.AGENT;

public class ExternalEventProcessManager {

    public static final String FIELD_AGENT_ID = "agentId";

    private static final Logger log = LoggerFactory.getLogger(ExternalEventProcessManager.class);

    AllocationHelper allocationHelper;
    InstanceDao instanceDao;
    ObjectProcessManager processManager;
    ObjectManager objectManager;
    ServiceDao serviceDao;
    LockManager lockManager;
    ResourceMonitor resourceMonitor;
    GenericResourceDao resourceDao;
    SchemaFactory schemaFactory;
    StackDao stackDao;


    public ExternalEventProcessManager(AllocationHelper allocationHelper, InstanceDao instanceDao, ObjectProcessManager processManager,
            ObjectManager objectManager, ServiceDao serviceDao, LockManager lockManager, ResourceMonitor resourceMonitor, GenericResourceDao resourceDao,
            SchemaFactory schemaFactory, StackDao stackDao) {
        super();
        this.allocationHelper = allocationHelper;
        this.instanceDao = instanceDao;
        this.processManager = processManager;
        this.objectManager = objectManager;
        this.serviceDao = serviceDao;
        this.lockManager = lockManager;
        this.resourceMonitor = resourceMonitor;
        this.resourceDao = resourceDao;
        this.schemaFactory = schemaFactory;
        this.stackDao = stackDao;
    }

    public HandlerResult preCreate(ProcessState state, ProcessInstance process) {
        // event's account id is set to the agent that submitted. This will change it to the actual user's account id.
        ExternalEvent event = (ExternalEvent)state.getResource();

        List<Agent> agents = objectManager.find(Agent.class, AGENT.ACCOUNT_ID, event.getAccountId());
        if (agents.size() == 1) {
            Agent agent = agents.get(0);
            Long resourceAccId = agent.getResourceAccountId();
            Map<String, Object> data = new HashMap<>();
            if (resourceAccId != null) {
                data.put(ObjectMetaDataManager.ACCOUNT_FIELD, resourceAccId);
            }
            if (event.getReportedAccountId() != null) {
                data.put(ExternalEventConstants.FIELD_REPORTED_ACCOUNT_ID, event.getReportedAccountId());
            } else {
                data.put(ExternalEventConstants.FIELD_REPORTED_ACCOUNT_ID, event.getAccountId());
            }

            return new HandlerResult(data);
        }

        return null;
    }

    public HandlerResult create(ProcessState state, ProcessInstance process) {
        ExternalEvent event = (ExternalEvent)state.getResource();

        if (StringUtils.isEmpty(event.getExternalId())) {
            log.debug("External event doesn't have an external id: {}", event.getId());
            return null;
        }

        switch (event.getKind()) {
        case ExternalEventConstants.KIND_EXTERNAL_DNS_EVENT:
            handleExternalDnsEvent(event, state, process);
            break;
        case ExternalEventConstants.KIND_EXTERNAL_HOST_EVENT:
            if (ExternalEventConstants.TYPE_HOST_EVACUATE.equals(event.getEventType())) {
                handleHostEvacuate(event);
            }
            break;
        case ExternalEventConstants.KIND_SERVICE_EVENT:
            handleServiceEvent(event);
            break;
        }

        return null;
    }

    private void handleExternalDnsEvent(final ExternalEvent event, ProcessState state, ProcessInstance process) {
        String fqdn = DataAccessor.fieldString(event, ExternalEventConstants.FIELD_FQDN);
        String serviceName = DataAccessor.fieldString(event, ExternalEventConstants.FIELD_SERVICE_NAME);
        String stackName = DataAccessor.fieldString(event, ExternalEventConstants.FIELD_STACK_NAME);
        if (fqdn == null || serviceName == null || stackName == null) {
            log.info("External DNS [event: " + event.getId() + "] misses some fields");
            return;
        }

        Stack stack = objectManager.findAny(Stack.class,
                STACK.ACCOUNT_ID, event.getAccountId(),
                STACK.REMOVED, null,
                STACK.NAME, stackName);
        if (stack == null) {
            log.info("Stack not found for external DNS [event: " + event.getId() + "]");
            return;
        }

        Service service = objectManager.findAny(Service.class,
                SERVICE.ACCOUNT_ID, event.getAccountId(),
                SERVICE.REMOVED, null,
                SERVICE.STACK_ID, stack.getId(),
                SERVICE.NAME, serviceName);
        if (service == null) {
            log.info("Service not found for external DNS [event: " + event.getId() + "]");
            return;
        }

        resourceDao.updateAndSchedule(service,
                ExternalEventConstants.FIELD_FQDN, fqdn);
    }

    private void handleHostEvacuate(ExternalEvent event) {
        boolean delete = DataAccessor.fieldBool(event, ExternalEventConstants.FIELD_DELETE_HOST);
        List<Long> hosts = getHosts(event);

        for (long hostId : hosts) {
            Host host = objectManager.loadResource(Host.class, hostId);
            if (host == null) {
                continue;
            }

            try {
                deactivateHost(host);
            } catch (ProcessCancelException e) {
            }

            if (delete) {
                host = objectManager.reload(host);
                try {
                    processManager.remove(host, null);
                } catch (ProcessCancelException e) {
                }
            }
        }
    }

    private List<Long> getHosts(ExternalEvent event) {
        List<Long> hosts = new ArrayList<>();

        String label = DataAccessor.fieldString(event, ExternalEventConstants.FIELD_HOST_LABEL);
        if (StringUtils.isNotBlank(label)) {
            Map<String, String> labels = new HashMap<>();
            labels.put(HostAffinityConstraint.LABEL_HEADER_AFFINITY_HOST_LABEL, DataAccessor.fieldString(event, ExternalEventConstants.FIELD_HOST_LABEL));
            hosts.addAll(allocationHelper.getAllHostsSatisfyingHostAffinity(event.getClusterId(), labels));
        }

        Long hostId = DataAccessor.fieldLong(event, ExternalEventConstants.FIELD_HOST_ID);
        if (hostId != null) {
            hosts.add(hostId);
        }

        return hosts;
    }

    private void deactivateHost(Host host) {
        processManager.executeDeactivate(host, null);

        List<? extends Instance> instances = instanceDao.getNonRemovedInstanceOn(host.getId());
        List<Instance> removed = new ArrayList<>();
        for (Instance instance : instances ) {
            if (InstanceConstants.isSystem(instance)) {
                continue;
            }
            try {
                processManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP, instance,
                        ProcessUtils.chainInData(new HashMap<>(), InstanceConstants.PROCESS_STOP,
                                InstanceConstants.PROCESS_REMOVE));
            } catch (ProcessCancelException e) {
            }

            removed.add(instance);
        }

        for (Instance instance : removed) {
            resourceMonitor.waitFor(instance, new ResourcePredicate<Instance>() {
                @Override
                public boolean evaluate(Instance obj) {
                    return obj.getRemoved() != null;
                }

                @Override
                public String getMessage() {
                    return "removed";
                }
            });
        }
    }

    private void handleServiceEvent(ExternalEvent event) {
        lockManager.lock(new ExternalEventLock(SERVICE_LOCK_NAME, event.getAccountId(), event.getExternalId()), () -> {
            Map<String, Object> serviceData = CollectionUtils.toMap(DataAccessor.getFields(event).get(FIELD_SERVICE));
            if (serviceData.isEmpty()) {
                log.warn("Empty service for externalServiceEvent: {}.", event);
                return null;
            }

            String kind = serviceData.get(ObjectMetaDataManager.KIND_FIELD) != null ? serviceData.get(ObjectMetaDataManager.KIND_FIELD).toString() : null;
            if (StringUtils.isEmpty(kind) || schemaFactory.getSchema(kind) == null) {
                log.warn("Couldn't find schema for service type [{}]. Returning.", kind);
                return null;
            }

            if (StringUtils.equals(event.getEventType(), TYPE_SERVICE_CREATE)) {
                createService(event, serviceData);
            } else if (StringUtils.equals(event.getEventType(), TYPE_SERVICE_UPDATE)) {
                updateService(event, serviceData);
            } else if (StringUtils.equals(event.getEventType(), TYPE_SERVICE_DELETE)) {
                deleteService(event, serviceData);
            } else if (StringUtils.equals(event.getEventType(), TYPE_STACK_DELETE)) {
                deleteStack(event, serviceData);
            }

            return null;
        });
    }

    private void createService(ExternalEvent event, Map<String, Object> serviceData) {
        Service svc = serviceDao.getServiceByExternalId(event.getAccountId(), event.getExternalId());
        if (svc != null) {
            return;
        }

        Stack stack = getStack(event);
        if (stack == null) {
            log.info("Can't process service event. Could not get or create stack. Event: [{}]", event);
            return;
        }

        Map<String, Object> service = new HashMap<>();
        if (serviceData != null) {
            service.putAll(serviceData);
        }
        service.put(ObjectMetaDataManager.ACCOUNT_FIELD, event.getAccountId());
        service.put(FIELD_STACK_ID, stack.getId());

        try {
            String create = processManager.getStandardProcessName(StandardProcess.CREATE, Service.class);
            String activate = processManager.getStandardProcessName(StandardProcess.ACTIVATE, Service.class);
            ProcessUtils.chainInData(service, create, activate);
            resourceDao.createAndSchedule(Service.class, service);
        } catch (ProcessCancelException e) {
            log.info("Create and activate process cancelled for service with account id {}and external id {}",
                    event.getAccountId(), event.getExternalId());
        }
    }

    private Stack getStack(final ExternalEvent event) {
        final Map<String, Object> env = CollectionUtils.castMap(DataAccessor.getFields(event).get(FIELD_ENVIRIONMENT));
        Object eId = CollectionUtils.getNestedValue(env, FIELD_EXTERNAL_ID);
        if (eId == null) {
            return null;
        }
        final String envExtId = eId.toString();

        Stack stack = stackDao.getStackByExternalId(event.getAccountId(), envExtId);
         //If stack has not been created yet
        if (stack == null) {
            final Stack newEnv = objectManager.newRecord(Stack.class);

            Object possibleName = CollectionUtils.getNestedValue(env, "name");
            newEnv.setExternalId(envExtId);
            newEnv.setAccountId(event.getAccountId());
            String name = possibleName != null ? possibleName.toString() : envExtId;
            newEnv.setName(name);

            stack = DeferredUtils.nest(new Callable<Stack>() {
                @Override
                public Stack call() {
                    return resourceDao.createAndSchedule(newEnv);
                }
            });

            stack = resourceMonitor.waitFor(stack, new ResourcePredicate<Stack>() {
                @Override
                public boolean evaluate(Stack obj) {
                    return obj != null && CommonStatesConstants.ACTIVE.equals(obj.getState());
                }

                @Override
                public String getMessage() {
                    return "active state";
                }
            });
        }
        return stack;
    }

    private void updateService(ExternalEvent event, Map<String, Object> serviceData) {
        Service svc = serviceDao.getServiceByExternalId(event.getAccountId(), event.getExternalId());
        if (svc == null) {
            log.info("Unable to find service while attempting to update. Returning. Service external id: [{}], account id: [{}]", event.getExternalId(),
                    event.getAccountId());
            return;
        }

        Map<String, Object> fields = DataAccessor.getFields(svc);
        Map<String, Object> updates = new HashMap<>();
        for (Map.Entry<String, Object> resourceField : serviceData.entrySet()) {
            String fieldName = resourceField.getKey();
            Object newFieldValue = resourceField.getValue();
            Object currentFieldValue = fields.get(fieldName);
            if (ObjectUtils.hasWritableProperty(svc, fieldName)) {
                Object property = ObjectUtils.getProperty(svc, fieldName);
                if (newFieldValue != null && !newFieldValue.equals(property) || property == null) {
                    updates.put(fieldName, newFieldValue);
                }
            } else if ((newFieldValue != null && !newFieldValue.equals(currentFieldValue)) || currentFieldValue != null) {
                updates.put(fieldName, newFieldValue);
            }
        }

        if (!updates.isEmpty()) {
            objectManager.setFields(svc, updates);
            resourceDao.updateAndSchedule(svc);
        }
    }

    private void deleteService(ExternalEvent event, Map<String, Object> serviceData) {
        Service svc = serviceDao.getServiceByExternalId(event.getAccountId(), event.getExternalId());
        if (svc != null) {
            processManager.scheduleStandardProcess(StandardProcess.REMOVE, svc, null);
        }
    }

    private void deleteStack(ExternalEvent event, Map<String, Object> stackData) {
        Stack env = stackDao.getStackByExternalId(event.getAccountId(), event.getExternalId());
        if (env != null) {
            processManager.scheduleStandardProcess(StandardProcess.REMOVE, env, null);
        }
    }

}
