package io.cattle.platform.register.process;

import static io.cattle.platform.core.model.tables.CredentialTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.register.util.RegisterConstants;

import javax.inject.Inject;
import javax.inject.Named;

import com.netflix.config.DynamicStringListProperty;

@Named
public class RegisterTokenAccountCreate extends AbstractObjectProcessLogic implements ProcessPostListener {

    public static final DynamicStringListProperty ACCOUNT_KINDS = ArchaiusUtil.getList("process.account.create.register.token.account.kinds");

    GenericResourceDao resourceDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { "account.create" };
    }

    @Override
    public HandlerResult handle(final ProcessState state, ProcessInstance process) {
        Account account = (Account) state.getResource();

        if (!ACCOUNT_KINDS.get().contains(account.getKind())) {
            return null;
        }

        boolean found = false;
        for (Credential cred : children(account, Credential.class)) {
            if (RegisterConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN.equals(cred.getKind())) {
                found = true;
                break;
            }
        }

        if (!found) {
            resourceDao.createAndSchedule(Credential.class, CREDENTIAL.ACCOUNT_ID, account.getId(), CREDENTIAL.KIND,
                    RegisterConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN);
        }

        return null;
    }

    public GenericResourceDao getResourceDao() {
        return resourceDao;
    }

    @Inject
    public void setResourceDao(GenericResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

}
