package io.cattle.platform.systemstack.process;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.systemstack.listener.SystemStackUpdate;

import java.io.IOException;

import javax.inject.Inject;

public class SystemStackAccountCreate extends AbstractObjectProcessLogic implements ProcessPostListener {


    @Inject
    SystemStackUpdate update;

    @Override
    public String[] getProcessNames() {
        return new String[] { "account.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        try {
            return handleInternal(state, process);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call catalog service: " + e.getMessage(), e);
        }
    }

    public HandlerResult handleInternal(ProcessState state, ProcessInstance process) throws IOException {
        Account account = (Account)state.getResource();
        update.createStacks(account);
        return null;
    }

}
