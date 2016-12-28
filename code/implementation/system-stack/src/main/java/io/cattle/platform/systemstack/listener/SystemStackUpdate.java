package io.cattle.platform.systemstack.listener;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.addon.CatalogTemplate;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ProjectTemplateConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ExternalHandler;
import io.cattle.platform.core.model.ProjectTemplate;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.lock.EventLock;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.systemstack.catalog.CatalogService;
import io.cattle.platform.systemstack.process.SystemStackTrigger;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;

public class SystemStackUpdate extends AbstractJooqDao implements AnnotatedEventListener {

    private static final DynamicBooleanProperty LAUNCH_COMPOSE_EXECUTOR = ArchaiusUtil.getBoolean("compose.executor.execute");

    private static final Logger log = LoggerFactory.getLogger(SystemStackUpdate.class);

    public static final String KUBERNETES = "k8s";
    public static final String SWARM = "swarm";
    public static final String MESOS = "mesos";
    public static final String WINDOWS = "windows";
    public static final String VIRTUAL_MACHINE = "virtualMachine";
    public static final List<String> ORC_PRIORITY = Arrays.asList(
            KUBERNETES,
            SWARM,
            MESOS,
            WINDOWS
            );
    public static final Set<String> ORCS = new HashSet<>(ORC_PRIORITY);

    @Inject
    ConfigItemStatusManager itemManager;
    @Inject
    EventService eventService;
    @Inject
    ObjectManager objectManager;
    @Inject
    HostDao hostDao;
    @Inject
    ObjectProcessManager processManager;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    CatalogService catalogService;
    @Inject
    ResourceMonitor resourceMonitor;

    @EventHandler(lock=EventLock.class)
    public void globalServiceUpdate(ConfigUpdate update) {
        if (update.getResourceId() == null) {
            return;
        }

        final Client client = new Client(Account.class, new Long(update.getResourceId()));
        itemManager.runUpdateForEvent(SystemStackTrigger.STACKS, update, client, new Runnable() {
            @Override
            public void run() {
                try {
                    process(client.getResourceId());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    protected void startStacks(Account account) throws IOException {
        if (account.getRemoved() != null) {
            return;
        }

        List<Long> createdStackIds = DataAccessor.fieldLongList(account, AccountConstants.FIELD_CREATED_STACKS);
        List<Long> startedStackIds = DataAccessor.fieldLongList(account, AccountConstants.FIELD_STARTED_STACKS);
        if (!startedStackIds.isEmpty() || createdStackIds.isEmpty()) {
            return;
        }

        if (!hostDao.hasActiveHosts(account.getId())) {
            return;
        }

        startedStackIds = new ArrayList<>();
        for (Long stackId : createdStackIds) {
            Stack stack = objectManager.loadResource(Stack.class, stackId);
            if (stack == null) {
                continue;
            }

            stack = resourceMonitor.waitForNotTransitioning(stack);
            if (CommonStatesConstants.ACTIVE.equals(stack.getState())) {
                for (Service service : objectManager.find(Service.class, SERVICE.STACK_ID, stackId, SERVICE.REMOVED, null)) {
                    processManager.scheduleStandardProcess(StandardProcess.ACTIVATE, service, null);
                }
            }

            startedStackIds.add(stackId);
        }

        objectManager.setFields(account, AccountConstants.FIELD_STARTED_STACKS, startedStackIds);
    }

    public static String chooseOrchestration(List<String> externalIds) {
        String orchestration = "cattle";
        Set<String> installedOrcs = new HashSet<>();
        for (String externalId : externalIds) {
            String orcType = getStackTypeFromExternalId(externalId);
            if (ORCS.contains(orcType)) {
                installedOrcs.add(orcType);
            }
        }

        for (String orc : ORC_PRIORITY) {
            if (installedOrcs.contains(orc)) {
                if (KUBERNETES.equals(orc)) {
                    orchestration = "kubernetes";
                } else {
                    orchestration = orc;
                }
            }
        }

        return orchestration;
    }

    protected void process(long accountId) throws IOException {
        Account account = objectManager.loadResource(Account.class, accountId);
        if (account == null || account.getRemoved() != null) {
            return;
        }

        createStacks(account);
        startStacks(account);

        List<Stack> stacks = objectManager.find(Stack.class,
                STACK.ACCOUNT_ID, account.getId(),
                STACK.REMOVED, null,
                STACK.EXTERNAL_ID, new Condition(ConditionType.LIKE, "%:infra*%"));

        boolean virtualMachine = false;
        List<String> externalIds = new ArrayList<>();
        for (Stack stack : stacks) {
            if (!ServiceConstants.isSystem(stack) || CommonStatesConstants.REMOVING.equals(stack.getState())) {
                continue;
            }

            externalIds.add(stack.getExternalId());
            String orcType = getStackTypeFromExternalId(stack.getExternalId());
            if (VIRTUAL_MACHINE.equals(orcType)) {
                virtualMachine = true;
            }
        }

        String orchestration = chooseOrchestration(externalIds);

        boolean oldVm = DataAccessor.fieldBool(account, AccountConstants.FIELD_VIRTUAL_MACHINE);
        String oldOrch = DataAccessor.fieldString(account, AccountConstants.FIELD_ORCHESTRATION);
        if (oldVm != virtualMachine || !ObjectUtils.equals(oldOrch, orchestration)) {
            objectManager.setFields(account,
                    AccountConstants.FIELD_ORCHESTRATION, orchestration,
                    AccountConstants.FIELD_VIRTUAL_MACHINE, virtualMachine);
            io.cattle.platform.object.util.ObjectUtils.publishChanged(eventService, account.getId(),
                    account.getId(), AccountConstants.TYPE);
        }
    }

    public static String getStackTypeFromExternalId(String externalId) {
        externalId = StringUtils.removeStart(externalId, "catalog://");
        String[] parts = externalId.split(":");
        if (parts.length < 2) {
            return null;
        }
        return StringUtils.removeStart(parts[1], "infra*");
    }

    public List<Long> createStacks(Account account) throws IOException {
        List<Long> createdStackIds = DataAccessor.fieldLongList(account, AccountConstants.FIELD_CREATED_STACKS);
        if (!createdStackIds.isEmpty()) {
            return createdStackIds;
        }

        ProjectTemplate projectTemplate = objectManager.loadResource(ProjectTemplate.class, account.getProjectTemplateId());
        if (projectTemplate == null) {
            return Collections.emptyList();
        }

        List<CatalogTemplate> templates = DataAccessor.fieldObjectList(projectTemplate, ProjectTemplateConstants.FIELD_STACKS,
                CatalogTemplate.class, jsonMapper);
        Map<String, CatalogTemplate> templatesById = catalogService.resolvedExternalIds(templates);
        createdStackIds = new ArrayList<>();

        boolean executorRunning = false;
        for (Map.Entry<String, CatalogTemplate> entry : templatesById.entrySet()) {
            String externalId = entry.getKey();
            Stack stack = objectManager.findAny(Stack.class,
                    STACK.ACCOUNT_ID, account.getId(),
                    STACK.EXTERNAL_ID, externalId,
                    STACK.REMOVED, null);

            if (stack == null) {
                executorRunning = waitForExecutor(executorRunning);
                stack = catalogService.deploy(account.getId(), entry.getValue());
            }

            createdStackIds.add(stack.getId());
        }

        objectManager.reload(account);
        objectManager.setFields(account, AccountConstants.FIELD_CREATED_STACKS, createdStackIds);
        return createdStackIds;
    }

    private synchronized boolean waitForExecutor(boolean executorRunning) {
        if (!LAUNCH_COMPOSE_EXECUTOR.get()) {
            return true;
        }

        if (executorRunning) {
            return executorRunning;
        }

        for (int i = 0; i < 120; i++) {
            ExternalHandler handler = objectManager.findAny(ExternalHandler.class,
                    ObjectMetaDataManager.NAME_FIELD, "rancher-compose-executor",
                    ObjectMetaDataManager.STATE_FIELD, CommonStatesConstants.ACTIVE);
            if (handler != null) {
                return true;
            }
            log.info("Waiting for rancher-compose-executor");

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        throw new TimeoutException("Failed to find rancher-compose-executor");
    }

}