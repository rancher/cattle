package io.cattle.platform.process.account;

import static io.cattle.platform.core.model.tables.CredentialTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.util.ProcessHelpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringProperty;

@Named
public class AccountCreate extends AbstractDefaultProcessHandler {

    public static final DynamicBooleanProperty CREATE_CREDENTIAL = ArchaiusUtil.getBoolean("process.account.create.create.credential");

    public static final DynamicStringProperty CREDENTIAL_TYPE = ArchaiusUtil.getString("process.account.create.create.credential.default.kind");

    public static final DynamicStringListProperty ACCOUNT_KIND_CREDENTIALS = ArchaiusUtil.getList("process.account.create.create.credential.account.kinds");

    ObjectProcessManager processManager;

    @Inject
    AccountDao accountDao;

    @Inject
    JsonMapper jsonMapper;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Account account = (Account) state.getResource();
        Map<Object, Object> result = new HashMap<Object, Object>();
        boolean createApiKey = DataAccessor.fromMap(state.getData()).withScope(AccountConstants.class).withKey(AccountConstants.OPTION_CREATE_APIKEY)
                .withDefault(false).as(Boolean.class);

        String apiKeyKind = DataAccessor.fromMap(state.getData()).withScope(AccountConstants.class).withKey(AccountConstants.OPTION_CREATE_APIKEY_KIND)
                .withDefault(CREDENTIAL_TYPE.get()).as(String.class);

        if (shouldCreateCredentials(account, state)) {
            if (createApiKey || CREATE_CREDENTIAL.get()) {
                List<Credential> creds = ProcessHelpers.createOneChild(getObjectManager(), processManager, account, Credential.class, CREDENTIAL.ACCOUNT_ID,
                        account.getId(), CREDENTIAL.KIND, apiKeyKind);

                for (Credential cred : creds) {
                    result.put("_createdCredential" + cred.getId(), true);
                }
            }
        }
        
        List<? extends Long> accountLinks = DataAccessor.fromMap(state.getData()).withKey(
                AccountConstants.FIELD_ACCOUNT_LINKS).withDefault(Collections.EMPTY_LIST)
            .asList(jsonMapper, Long.class);
        
        accountDao.generateAccountLinks(account, accountLinks);

        return new HandlerResult(result);
    }

    public boolean shouldCreateCredentials(Account account, ProcessState state) {
        return ACCOUNT_KIND_CREDENTIALS.get().contains(account.getKind());
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

}
