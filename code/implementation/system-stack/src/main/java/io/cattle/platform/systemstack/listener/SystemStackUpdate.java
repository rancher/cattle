package io.cattle.platform.systemstack.listener;

import static io.cattle.platform.core.model.tables.StackTable.*;

import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.lock.EventLock;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.systemstack.process.SystemStackTrigger;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class SystemStackUpdate extends AbstractJooqDao implements AnnotatedEventListener {

    public static final String KUBERNETES = "k8s";
    public static final String SWARM = "swarm";
    public static final String MESOS = "mesos";
    public static final String VIRTUAL_MACHINE = "virtualMachine";
    public static final List<String> ORC_PRIORITY = Arrays.asList(
            KUBERNETES,
            SWARM,
            MESOS
            );
    public static final Set<String> ORCS = new HashSet<>(ORC_PRIORITY);

    @Inject
    ConfigItemStatusManager itemManager;

    @Inject
    EventService eventService;

    @Inject
    ObjectManager objectManager;

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

    protected void process(long accountId) throws IOException {
        Account account = objectManager.loadResource(Account.class, accountId);
        if (account == null || account.getRemoved() != null) {
            return;
        }

        List<Stack> stacks = objectManager.find(Stack.class,
                STACK.ACCOUNT_ID, account.getId(),
                STACK.REMOVED, null,
                STACK.EXTERNAL_ID, new Condition(ConditionType.LIKE, "%:infra*%"));

        boolean virtualMachine = false;
        String orchestration = "cattle";
        Set<String> installedOrcs = new HashSet<>();
        for (Stack stack : stacks) {
            if (!ServiceConstants.isSystem(stack)) {
                continue;
            }

            String orcType = getStackTypeFromExternalId(stack.getExternalId());
            if (ORCS.contains(orcType)) {
                installedOrcs.add(orcType);
            }

            if (VIRTUAL_MACHINE.equals(orcType)) {
                virtualMachine = true;
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

        boolean oldVm = DataAccessor.fieldBool(account, AccountConstants.FIELD_ORCHESTRATION);
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
        String[] parts = externalId.split(":");
        if (parts.length < 4) {
            return null;
        }
        return StringUtils.removeStart(parts[2], "infra*");
    }

}
