package io.github.ibuildthecloud.dstack.process.account;

import static io.github.ibuildthecloud.dstack.core.model.tables.CredentialTable.*;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.model.Account;
import io.github.ibuildthecloud.dstack.core.model.Credential;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.dstack.process.base.AbstractDefaultProcessHandler;
import io.github.ibuildthecloud.dstack.process.util.ProcessHelpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

@Named
public class AccountCreate extends AbstractDefaultProcessHandler {

    public static final DynamicBooleanProperty CREATE_CREDENTIAL = ArchaiusUtil
            .getBoolean("process.account.create.create.credential");

    public static final DynamicStringProperty CREDENTIAL_TYPE = ArchaiusUtil
            .getString("process.account.create.create.credential.default.kind");

    ObjectProcessManager processManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Account account = (Account) state.getResource();
        Map<Object,Object> result = new HashMap<Object, Object>();

        if (CREATE_CREDENTIAL.get()) {
            List<Credential> creds = ProcessHelpers.createOneChild(getObjectManager(), processManager, account, Credential.class,
                    CREDENTIAL.ACCOUNT_ID, account.getId(),
                    CREDENTIAL.KIND, CREDENTIAL_TYPE.get());

            for (Credential cred : creds) {
                result.put("_createdCredential" + cred.getId(), true);
            }
        }

        return new HandlerResult(result);
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

}
