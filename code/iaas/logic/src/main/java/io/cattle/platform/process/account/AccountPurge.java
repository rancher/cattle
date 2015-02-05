package io.cattle.platform.process.account;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Named;

@Named
public class AccountPurge extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Account account = (Account) state.getResource();

        for (Credential cred : getObjectManager().children(account, Credential.class)) {
            try {
                deactivateThenRemove(cred, state.getData());
            } catch (ProcessCancelException e) {
                // ignore
            }
        }

        for (Host host : getObjectManager().children(account, Host.class)) {
            try {
                deactivateThenRemove(host, state.getData());
            } catch (ProcessCancelException e) {
                // ignore
            }
        }

        return null;
    }

}
