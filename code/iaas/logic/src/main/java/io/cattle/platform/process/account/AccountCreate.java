package io.cattle.platform.process.account;

import static io.cattle.platform.core.model.tables.CredentialTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.util.ProcessHelpers;

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
