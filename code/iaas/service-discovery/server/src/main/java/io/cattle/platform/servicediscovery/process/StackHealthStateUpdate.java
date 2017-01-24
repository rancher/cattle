package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.lock.EventLock;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.servicediscovery.deployment.impl.lock.StackHealthStateUpdateLock;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.io.IOException;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class StackHealthStateUpdate implements AnnotatedEventListener {

    @Inject
    ObjectProcessManager processManager;

    @Inject
    ConfigItemStatusManager itemManager;

    @Inject
    EventService eventService;

    @Inject
    ObjectManager objectManager;

    @Inject
    HostDao hostDao;

    @Inject
    LockManager lockManager;

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ServiceDiscoveryService sdService;

    @EventHandler(lock = EventLock.class)
    public void stackUpdate(ConfigUpdate update) {
        if (update.getResourceId() == null) {
            return;
        }

        final Client client = new Client(Stack.class, new Long(update.getResourceId()));
        reconcileForClient(update, client, new Callable<Boolean>() {
            @Override
            public Boolean call() throws IOException {
                return process(client.getResourceId());
            }
        });
    }

    protected boolean process(long stackId) throws IOException {
        Stack stack = objectManager.loadResource(Stack.class, stackId);
        if (stack == null) {
            return true;
        }

        sdService.updateHealthState(stack);

        return true;
    }

    protected void reconcileForClient(final ConfigUpdate update, final Client client, final Callable<Boolean> run) {
        lockManager.lock(new StackHealthStateUpdateLock(client.getResourceId()), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                ItemVersion itemVersion = itemManager.getRequestedVersion(client, StackHealthStateUpdateTrigger.STACK);
                if (itemVersion == null) {
                    return;
                }
                try {
                    if (run.call()) {
                        itemManager.setApplied(client, StackHealthStateUpdateTrigger.STACK, itemVersion);
                        eventService.publish(EventVO.reply(update));
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to process stack health state update for ["
                            + client.getResourceId()
                            + "]", e);
                }
            }
        });
    }
}
