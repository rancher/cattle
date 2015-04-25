package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.ExternalIdHandler;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;

import javax.inject.Inject;

import com.netflix.config.DynamicBooleanProperty;

public class RancherExternalIdHandler implements ExternalIdHandler {

    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean("api.security.enabled");
    private static final DynamicBooleanProperty USE_RANCHER_IDS = ArchaiusUtil.getBoolean("api.projects.use.rancher_id");

    @Inject
    AuthDao authDao;
    @Inject
    ObjectManager objectManager;

    @Override
    public ExternalId transform(ExternalId externalId) {
        if (externalId.getType().equalsIgnoreCase(ProjectConstants.RANCHER_ID) && (!SECURITY.get() || USE_RANCHER_IDS.get())) {
            String accountId = ApiContext.getContext().getIdFormatter().parseId(externalId.getId());
            Account account = authDao.getAccountById(Long.valueOf(accountId));
            if (account != null){
                return  new ExternalId(String.valueOf(account.getId()), externalId.getType());
            }
        }
        return null;
    }

    @Override
    public ExternalId untransform(ExternalId externalId) {
        if (externalId.getType().equalsIgnoreCase(ProjectConstants.RANCHER_ID) && (!SECURITY.get() || USE_RANCHER_IDS.get())) {
            Account account = authDao.getAccountById(Long.valueOf(externalId.getId()));
            if (account != null){
                String accountId = (String) ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(Account.class), account.getId());
                return  new ExternalId(accountId, externalId.getType(), account.getName());
            }
        }
        return null;
    }
}
