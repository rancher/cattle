package io.cattle.platform.process.account;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AccountUpdate extends AbstractDefaultProcessHandler {

    @Inject
    AccountDao accountDao;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Account account = (Account) state.getResource();
        List<? extends Long> accountLinks = DataAccessor.fromMap(state.getData()).withKey(
                AccountConstants.FIELD_ACCOUNT_LINKS).withDefault(Collections.EMPTY_LIST)
                .asList(jsonMapper, Long.class);
        accountDao.generateAccountLinks(account, accountLinks);
        return null;
    }

}
