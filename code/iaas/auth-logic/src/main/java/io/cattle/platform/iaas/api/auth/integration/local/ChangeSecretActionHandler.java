package io.cattle.platform.iaas.api.auth.integration.local;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.iaas.api.auth.dao.PasswordDao;
import io.cattle.platform.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class ChangeSecretActionHandler implements ActionHandler {

    @Inject
    PasswordDao passwordDao;

    @Inject
    JsonMapper jsonMapper;

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Credential password = (Credential) obj;
        ChangePassword changePassword = request.proxyRequestObject(ChangePassword.class);
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        LocalAuthPasswordValidator.validatePassword(changePassword.getNewSecret(), jsonMapper);
        return passwordDao.changePassword(password, changePassword, policy);
    }

    @Override
    public String getName() {
        return "credential.changesecret";
    }
}
