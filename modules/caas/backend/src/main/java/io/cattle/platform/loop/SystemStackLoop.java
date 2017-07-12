package io.cattle.platform.loop;

import io.cattle.platform.core.addon.CatalogTemplate;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ProjectTemplateConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ProjectTemplate;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.systemstack.catalog.CatalogService;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;

public class SystemStackLoop implements Loop {

    long accountId;
    EventService eventService;
    ObjectManager objectManager;
    HostDao hostDao;
    ObjectProcessManager processManager;
    CatalogService catalogService;
    EnvironmentResourceManager envResourceManager;

    public SystemStackLoop(long accountId, EventService eventService, ObjectManager objectManager,
                           HostDao hostDao, ObjectProcessManager processManager, CatalogService catalogService,
                           EnvironmentResourceManager envResourceManager) {
        this.accountId = accountId;
        this.eventService = eventService;
        this.objectManager = objectManager;
        this.hostDao = hostDao;
        this.processManager = processManager;
        this.catalogService = catalogService;
        this.envResourceManager = envResourceManager;
    }

    protected void startStacks(Account account) {
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

            if (CommonStatesConstants.ACTIVE.equals(stack.getState())) {
                for (Service service : objectManager.find(Service.class, SERVICE.STACK_ID, stackId, SERVICE.REMOVED, null)) {
                    processManager.scheduleStandardProcess(StandardProcess.ACTIVATE, service, null);
                }
            }

            startedStackIds.add(stackId);
        }

        setFields(AccountConstants.FIELD_STARTED_STACKS, startedStackIds);
    }

    @Override
    public Result run(Object input) {
        Account account = objectManager.loadResource(Account.class, accountId);
        if (account == null || account.getRemoved() != null) {
            return Result.DONE;
        }

        try {
            createStacks(account);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create system stacks", e);
        }

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
            String orcType = AccountConstants.getStackTypeFromExternalId(stack.getExternalId());
            if (AccountConstants.VIRTUAL_MACHINE.equals(orcType)) {
                virtualMachine = true;
            }
        }

        String orchestration = AccountConstants.chooseOrchestration(externalIds);

        boolean oldVm = DataAccessor.fieldBool(account, AccountConstants.FIELD_VIRTUAL_MACHINE);
        String oldOrch = DataAccessor.fieldString(account, AccountConstants.FIELD_ORCHESTRATION);
        if (oldVm != virtualMachine || !Objects.equals(oldOrch, orchestration)) {
            setFields(AccountConstants.FIELD_ORCHESTRATION, orchestration,
                    AccountConstants.FIELD_VIRTUAL_MACHINE, virtualMachine);
            io.cattle.platform.object.util.ObjectUtils.publishChanged(eventService, account.getId(),
                    account.getId(), AccountConstants.TYPE);
        }

        return Result.DONE;
    }

    private Account setFields(Object key, Object... valueKeyValue) {
        return envResourceManager.getMetadata(accountId).modify(Account.class, accountId, (account) -> {
            return objectManager.setFields(account, key, valueKeyValue);
        });
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
                CatalogTemplate.class);
        Map<String, CatalogTemplate> templatesById = catalogService.resolvedExternalIds(templates);
        createdStackIds = new ArrayList<>();

        for (Map.Entry<String, CatalogTemplate> entry : templatesById.entrySet()) {
            String externalId = entry.getKey();
            Stack stack = objectManager.findAny(Stack.class,
                    STACK.ACCOUNT_ID, account.getId(),
                    STACK.EXTERNAL_ID, externalId,
                    STACK.REMOVED, null);

            if (stack == null) {
                stack = catalogService.deploy(account.getId(), entry.getValue());
            }

            createdStackIds.add(stack.getId());
        }

        setFields(AccountConstants.FIELD_CREATED_STACKS, createdStackIds);
        return createdStackIds;
    }

}