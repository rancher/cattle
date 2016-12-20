package io.cattle.platform.systemstack.process;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.systemstack.listener.SystemStackUpdate;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class SystemStackAccountCreate extends AbstractObjectProcessLogic implements ProcessPostListener {


    @Inject
    SystemStackUpdate update;

    @Override
    public String[] getProcessNames() {
        return new String[] { "account.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Account account = (Account)state.getResource();
        DeferredUtils.defer(new Runnable() {
            @Override
            public void run() {
                try {
                    update.createStacks(account);
                } catch (IOException e) {
                }
            }
        });
        return null;
    }
}
