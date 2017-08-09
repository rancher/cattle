package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.AccountLinkTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.core.model.Region;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class RegionRemove extends AbstractObjectProcessHandler {
    @Inject
    LockManager lockManager;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public String[] getProcessNames() {
        return new String[] { "region.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Region region = (Region) state.getResource();
        cleanupExternalLinks(region);
        return null;
    }

    private void cleanupExternalLinks(Region region) {
        for (AccountLink link : objectManager.find(AccountLink.class, ACCOUNT_LINK.REMOVED, null, ACCOUNT_LINK.LINKED_REGION_ID, region.getId())) {
            if (!link.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
                objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, link, null);
            }
        }
    }
}