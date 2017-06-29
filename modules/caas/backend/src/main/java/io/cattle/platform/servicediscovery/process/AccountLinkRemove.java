package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.AccountLinkTable.*;
import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.lock.AccountLinksUpdateLock;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;

public class AccountLinkRemove implements ProcessHandler {

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    LockManager lockManager;
    EventService eventService;

    public AccountLinkRemove(ObjectManager objectManager, ObjectProcessManager processManager, LockManager lockManager, EventService eventService) {
        super();
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.lockManager = lockManager;
        this.eventService = eventService;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        AccountLink accountLink = (AccountLink) state.getResource();
        updateServices(accountLink);
        Account account = objectManager.loadResource(Account.class, accountLink.getAccountId());
        if (account == null) {
            return null;
        }
        regenerateAccountLinks(account);
        return null;
    }

    @SuppressWarnings("unchecked")
    private void updateServices(AccountLink accountLink) {
        List<? extends ServiceConsumeMap> consumeMaps = objectManager.find(ServiceConsumeMap.class,
                SERVICE_CONSUME_MAP.ACCOUNT_ID,
                accountLink.getAccountId(), SERVICE_CONSUME_MAP.REMOVED, null);
        List<? extends Service> services = objectManager.find(Service.class, SERVICE.ACCOUNT_ID,
                accountLink.getLinkedAccountId(), SERVICE.REMOVED, null);
        List<Long> serviceIds = (List<Long>) CollectionUtils.collect(services,
                TransformerUtils.invokerTransformer("getId"));
        for (ServiceConsumeMap consumeMap : consumeMaps) {
            if (serviceIds.contains(consumeMap.getConsumedServiceId())) {
                processManager.remove(consumeMap, null);
            }
        }
    }

    protected void regenerateAccountLinks(final Account account) {
        lockManager.lock(new AccountLinksUpdateLock(account.getId()), () -> {
            List<? extends AccountLink> allLinks = objectManager.find(AccountLink.class,
                    ACCOUNT_LINK.ACCOUNT_ID, account.getId(),
                    ACCOUNT_LINK.REMOVED, null, ACCOUNT_LINK.STATE, new Condition(ConditionType.NE,
                            CommonStatesConstants.REMOVING));
            List<Long> newLinks = new ArrayList<>();
            for (AccountLink aLink : allLinks) {
                newLinks.add(aLink.getLinkedAccountId());
            }

            List<Long> existingLinks = DataAccessor.fieldLongList(account,
                    AccountConstants.FIELD_ACCOUNT_LINKS);
            if (existingLinks.containsAll(newLinks) && newLinks.containsAll(existingLinks)) {
                return null;
            }

            objectManager.setFields(account, AccountConstants.FIELD_ACCOUNT_LINKS, newLinks);
            ObjectUtils.publishChanged(eventService, objectManager, account);

            return null;
        });
    }

}