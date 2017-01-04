package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AccountLinkCreate extends AbstractObjectProcessLogic implements ProcessPostListener {

    @Inject
    ServiceDiscoveryService sdSvc;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        AccountLink accountLink = (AccountLink) state.getResource();
        List<? extends Service> services = objectManager.find(Service.class, SERVICE.ACCOUNT_ID,
                accountLink.getAccountId(), SERVICE.SELECTOR_LINK, new Condition(ConditionType.NOTNULL),
                SERVICE.REMOVED, null);
        for (Service service : services) {
            sdSvc.registerServiceLinks(service);
        }
        return null;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "accountlink.create" };
    }

}
