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
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.process.common.lock.AccountLinksUpdateLock;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;

@Named
public class AccountLinkRemove extends AbstractObjectProcessLogic implements ProcessPostListener {

    @Inject
    ServiceDiscoveryService sdSvc;
    @Inject
    LockManager lockManager;
    @Inject
    EventService eventService;

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
                objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, consumeMap, null);
            }
        }
    }

    protected void regenerateAccountLinks(final Account account) {
        lockManager.lock(new AccountLinksUpdateLock(account.getId()),
                new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {
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
                            return;
                        }

                        objectManager.setFields(account, AccountConstants.FIELD_ACCOUNT_LINKS, newLinks);
                        ObjectUtils.publishChanged(eventService, objectManager, account);
                    }
                });
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "accountlink.remove" };
    }

}