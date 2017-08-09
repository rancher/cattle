package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.AccountTable.*;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Region;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.service.RegionService;
import io.cattle.platform.util.type.Priority;

import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class RegionsAccountLinksReconcile extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    RegionService regionService;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_SERVICE_CONSUME_MAP_CREATE, ServiceConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                ServiceConstants.PROCESS_SERVICE_REMOVE,
                ServiceConstants.PROCESS_SERVICE_UPDATE, ServiceConstants.PROCESS_SERVICE_CREATE, "region.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object obj = state.getResource();
        List<Long> accountIds = new ArrayList<>();
        if (obj instanceof Region) {
            for (Account account : objectManager.find(Account.class, ACCOUNT.KIND, AccountConstants.PROJECT_KIND, ACCOUNT.REMOVED,
                    new Condition(ConditionType.NULL))) {
                accountIds.add(account.getId());
            }
        } else if (obj instanceof Account) {
            accountIds.add(((Account) obj).getId());
        } else {
            Object accountId = ObjectUtils.getAccountId(obj);
            if (accountId != null) {
                accountIds.add((Long) accountId);
            }
        }

        for (Long accountId : accountIds) {
            regionService.reconcileExternalLinks(accountId);
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
