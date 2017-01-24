package io.cattle.platform.process.network;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.process.lock.DefaultNetworkLock;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.ObjectUtils;

@Named
public class DefaultNetworkProcess extends AbstractObjectProcessHandler {

    @Inject
    NetworkDao networkDao;

    @Inject
    LockManager lockManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Network network = (Network)state.getResource();
        lockManager.lock(new DefaultNetworkLock(network), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                setDefaultNetwork(network.getAccountId());
            }
        });
        return null;
    }

    protected void setDefaultNetwork(Long accountId) {
        Account account = objectManager.loadResource(Account.class, accountId);
        if (account == null) {
            return;
        }

        Long defaultNetworkId = account.getDefaultNetworkId();
        Long newDefaultNetworkId = null;
        for (Network network : networkDao.getActiveNetworks(account.getId())) {
            if (network.getKind().startsWith(NetworkConstants.PREFIX_KIND_DOCKER) ||
                    network.getKind().equals("hostOnlyNetwork")) {
                continue;
            }

            if (network.getId().equals(defaultNetworkId)) {
                newDefaultNetworkId = defaultNetworkId;
                break;
            }

            if ((CommonStatesConstants.ACTIVATING.equals(network.getState()) ||
                    CommonStatesConstants.UPDATING_ACTIVE.equals(network.getState())) &&
                    newDefaultNetworkId == null) {
                newDefaultNetworkId = network.getId();
            } else if (CommonStatesConstants.ACTIVE.equals(network.getState())) {
                newDefaultNetworkId = network.getId();
            }
        }

        if (!ObjectUtils.equals(defaultNetworkId, newDefaultNetworkId)) {
            objectManager.setFields(account, AccountConstants.FIELD_DEFAULT_NETWORK_ID, newDefaultNetworkId);
        }
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "network.*" };
    }

}