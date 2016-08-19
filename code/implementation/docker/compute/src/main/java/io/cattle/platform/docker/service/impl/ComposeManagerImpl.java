package io.cattle.platform.docker.service.impl;

import static io.cattle.platform.core.model.tables.EnvironmentTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.docker.process.dao.ComposeDao;
import io.cattle.platform.docker.process.lock.ComposeProjectLock;
import io.cattle.platform.docker.process.lock.ComposeServiceLock;
import io.cattle.platform.docker.process.util.DockerConstants;
import io.cattle.platform.docker.service.ComposeManager;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.DefaultMultiLockDefinition;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.deployment.impl.unit.DefaultDeploymentUnitInstance;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class ComposeManagerImpl implements ComposeManager {

    public static final String SERVICE_LABEL = "com.docker.compose.service";
    public static final String SERVICE_ID = "com.docker.swarm.service.id";
    public static final String SWARM_SERVICE = "com.docker.swarm.service.name";
    public static final String PROJECT_LABEL = "com.docker.compose.project";

    @Inject
    ComposeDao composeDao;
    @Inject
    GenericResourceDao resourceDao;
    @Inject
    LockManager lockManager;
    @Inject
    ObjectManager objectManager;
    @Inject
    ServiceExposeMapDao serviceExportMapDao;
    @Inject
    ObjectProcessManager objectProcessManager;
    @Inject
    JsonMapper jsonMapper;

    protected String getString(Map<String, Object> labels, String key) {
        Object value = labels.get(key);
        return value == null ? null : value.toString();
    }

    @Override
    public void setupServiceAndInstance(Instance instance) {
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        String service = getString(labels, SERVICE_LABEL);
        if (service == null) {
            service = getString(labels, SWARM_SERVICE);
        }

        if (StringUtils.isBlank(service)) {
            return;
        }

        String project = getProject(instance, labels);
        String serviceId = getString(labels, SERVICE_ID);

        instance = setupLabels(instance, service, project);
        getService(instance, service, project, serviceId);
    }

    protected String getProject(Instance instance, Map<String, Object> labels) {
        String project = getString(labels, PROJECT_LABEL);
        if (project != null) {
            return project;
        }

        Map<String, Object> networks = CollectionUtils.toMap(CollectionUtils.getNestedValue(instance.getData(),
                "dockerContainer", "NetworkSettings", "Networks"));

        if (networks.size() == 0) {
            return "default";
        }

        String networkName = networks.keySet().iterator().next();
        return "bridge".equals(networkName) ? "default" : networkName;
    }

    private Instance setupLabels(Instance instance, String service, String project) {
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        setIfNot(labels, ServiceDiscoveryConstants.LABEL_SERVICE_DEPLOYMENT_UNIT, UUID.randomUUID());
        setIfNot(labels, ServiceDiscoveryConstants.LABEL_SERVICE_LAUNCH_CONFIG, ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        setIfNot(labels, ServiceDiscoveryConstants.LABEL_STACK_NAME, project);
        setIfNot(labels, ServiceDiscoveryConstants.LABEL_STACK_SERVICE_NAME, String.format("%s/%s", project, service));
        DataAccessor.setField(instance, InstanceConstants.FIELD_LABELS, labels);

        return objectManager.persist(instance);
    }

    protected void setIfNot(Map<String, Object> labels, String key, Object value) {
        if (!labels.containsKey(key)) {
            labels.put(key, value);
        }
    }

    protected Service createService(Instance instance, String project, String service, String serviceId) {
        Environment env = getEnvironment(instance.getAccountId(), project);
        Map<String, Object> instanceData = jsonMapper.writeValueAsMap(instance);
        instanceData.remove(ObjectMetaDataManager.ID_FIELD);
        instanceData.remove(ObjectMetaDataManager.STATE_FIELD);
        instanceData.remove("token");
        instanceData.remove(ObjectMetaDataManager.DATA_FIELD);
        instanceData.remove(ObjectMetaDataManager.CREATED_FIELD);
        Iterator<Entry<String, Object>> iter = instanceData.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, Object> entry = iter.next();
            if (entry.getValue() == null || entry.getValue() instanceof Number) {
                iter.remove();
            }
        }

        String selector = String.format("%s=%s", SERVICE_ID, serviceId);
        if (serviceId == null) {
            selector = String.format("%s=%s, %s=%s", PROJECT_LABEL, project, SERVICE_LABEL, service);
        }

        return resourceDao.createAndSchedule(Service.class,
                SERVICE.NAME, service,
                SERVICE.ACCOUNT_ID, instance.getAccountId(),
                SERVICE.ENVIRONMENT_ID, env.getId(),
                SERVICE.SELECTOR_CONTAINER, selector,
                ServiceDiscoveryConstants.FIELD_START_ON_CREATE, true,
                ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG, instanceData,
                SERVICE.KIND, "composeService");
    }

    protected Service getService(final Instance instance, final String name, final String projectName, final String serviceId) {
        Service service = composeDao.getComposeServiceByName(instance.getAccountId(), name, projectName);
        if (service != null) {
            return service;
        }

        return lockManager.lock(new ComposeServiceLock(instance.getAccountId(), name), new LockCallback<Service>() {
            @Override
            public Service doWithLock() {
                Service service = composeDao.getComposeServiceByName(instance.getAccountId(), name, projectName);
                if (service != null) {
                    return service;
                }

                return createService(instance, projectName, name, serviceId);
            }
        });
    }

    protected Environment getEnvironment(final long accountId, final String project) {
        Environment env = composeDao.getComposeProjectByName(accountId, project);
        if (env != null) {
            return env;
        }

        return lockManager.lock(new ComposeProjectLock(accountId, project), new LockCallback<Environment>() {
            @Override
            public Environment doWithLock() {
                Environment env = composeDao.getComposeProjectByName(accountId, project);
                if (env != null) {
                    return env;
                }

                return resourceDao.createAndSchedule(Environment.class,
                        ENVIRONMENT.NAME, project,
                        ENVIRONMENT.ACCOUNT_ID, accountId,
                        ENVIRONMENT.KIND, "composeProject");
            }
        });
    }

    @Override
    public void cleanupResources(final Service service) {
        if (!DockerConstants.TYPE_COMPOSE_SERVICE.equals(service.getKind())) {
            return;
        }

        final Environment env = objectManager.loadResource(Environment.class, service.getEnvironmentId());
        lockManager.lock(new DefaultMultiLockDefinition(new ComposeProjectLock(env.getAccountId(), env.getName()),
                new ComposeServiceLock(env.getAccountId(), service.getName())), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                checkAndDelete(service, env);
            }
        });
    }

    protected void checkAndDelete(Service service, Environment env) {
        service = objectManager.reload(service);
        env = objectManager.reload(env);

        boolean found = false;
        for (ServiceExposeMap map : serviceExportMapDao.getUnmanagedServiceInstanceMapsToRemove(service.getId())) {
            found = true;
            if (isRemoved(service.getRemoved(), service.getState())) {
                Instance instance = objectManager.loadResource(Instance.class, map.getInstanceId());
                DefaultDeploymentUnitInstance.removeInstance(instance, objectProcessManager);
            }
        }

        if (!found && !isRemoved(service.getRemoved(), service.getState())) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, service, null);
        }

        env = objectManager.reload(env);
        if (isRemoved(env.getRemoved(), env.getState())) {
            return;
        }

        List<Service> services = objectManager.find(Service.class,
                SERVICE.ENVIRONMENT_ID, env.getId(),
                ObjectMetaDataManager.STATE_FIELD, new Condition(ConditionType.NE, CommonStatesConstants.REMOVING),
                ObjectMetaDataManager.REMOVED_FIELD, null);
        if (services.size() == 0) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, env, null);
        }
    }

    protected boolean isRemoved(Date removed, String state) {
        if (removed != null) {
            return true;
        }
        return CommonStatesConstants.REMOVING.equals(state);
    }
}
