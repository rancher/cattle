package io.cattle.platform.docker.service.impl;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.Stack;
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
import io.cattle.platform.servicediscovery.deployment.impl.instance.AbstractDeploymentUnitInstance;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class ComposeManagerImpl implements ComposeManager {

    public static final String SERVICE_LABEL = "com.docker.compose.service";
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
        String project = getString(labels, PROJECT_LABEL);
        String service = getString(labels, SERVICE_LABEL);

        if (StringUtils.isBlank(project) || StringUtils.isBlank(service)) {
            return;
        }

        instance = setupLabels(instance, service, project);
        getService(instance, service, project);
    }

    private Instance setupLabels(Instance instance, String service, String project) {
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        setIfNot(labels, ServiceConstants.LABEL_SERVICE_DEPLOYMENT_UNIT, io.cattle.platform.util.resource.UUID.randomUUID());
        setIfNot(labels, ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG, ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        setIfNot(labels, ServiceConstants.LABEL_STACK_NAME, project);
        setIfNot(labels, ServiceConstants.LABEL_STACK_SERVICE_NAME, String.format("%s/%s", project, service));
        DataAccessor.setField(instance, InstanceConstants.FIELD_LABELS, labels);

        return objectManager.persist(instance);
    }

    protected void setIfNot(Map<String, Object> labels, String key, Object value) {
        if (!labels.containsKey(key)) {
            labels.put(key, value);
        }
    }

    protected Service createService(Instance instance) {
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        String project = getString(labels, PROJECT_LABEL);
        String service = getString(labels, SERVICE_LABEL);

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

        Stack stack = getStack(instance.getAccountId(), project);

        return resourceDao.createAndSchedule(Service.class,
                SERVICE.NAME, service,
                SERVICE.ACCOUNT_ID, instance.getAccountId(),
                SERVICE.STACK_ID, stack.getId(),
                SERVICE.SELECTOR_CONTAINER, String.format("%s=%s, %s=%s", PROJECT_LABEL, project, SERVICE_LABEL, service),
                ServiceConstants.FIELD_START_ON_CREATE, true,
                ServiceConstants.FIELD_LAUNCH_CONFIG, instanceData,
                SERVICE.KIND, "composeService");
    }

    protected Service getService(final Instance instance, final String name, final String projectName) {
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

                return createService(instance);
            }
        });
    }

    protected Stack getStack(final long accountId, final String project) {
        Stack env = composeDao.getComposeProjectByName(accountId, project);
        if (env != null) {
            return env;
        }

        return lockManager.lock(new ComposeProjectLock(accountId, project), new LockCallback<Stack>() {
            @Override
            public Stack doWithLock() {
                Stack env = composeDao.getComposeProjectByName(accountId, project);
                if (env != null) {
                    return env;
                }

                return resourceDao.createAndSchedule(Stack.class,
                        STACK.NAME, project,
                        STACK.ACCOUNT_ID, accountId,
                        STACK.KIND, "composeProject");
            }
        });
    }

    @Override
    public void cleanupResources(final Service service) {
        if (!DockerConstants.TYPE_COMPOSE_SERVICE.equals(service.getKind())) {
            return;
        }

        final Stack env = objectManager.loadResource(Stack.class, service.getStackId());
        lockManager.lock(new DefaultMultiLockDefinition(new ComposeProjectLock(env.getAccountId(), env.getName()),
                new ComposeServiceLock(env.getAccountId(), service.getName())), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                checkAndDelete(service, env);
            }
        });
    }

    protected void checkAndDelete(Service service, Stack env) {
        service = objectManager.reload(service);
        env = objectManager.reload(env);

        boolean found = false;
        for (ServiceExposeMap map : serviceExportMapDao.getUnmanagedServiceInstanceMapsToRemove(service.getId())) {
            found = true;
            if (isRemoved(service.getRemoved(), service.getState())) {
                Instance instance = objectManager.loadResource(Instance.class, map.getInstanceId());
                AbstractDeploymentUnitInstance.removeInstance(instance, objectProcessManager);
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
                SERVICE.STACK_ID, env.getId(),
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