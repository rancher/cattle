package io.cattle.platform.service.account;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.serialization.ObjectTypeSerializerPostProcessor;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.server.context.ServerContext.BaseProtocol;
import io.cattle.platform.service.launcher.ServiceAccountCreateStartup;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemRoleObjectPostProcessor implements ObjectTypeSerializerPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(SystemRoleObjectPostProcessor.class);

    @Inject
    ObjectManager objectManager;

    @Inject
    ServiceAccountCreateStartup serviceAccount;

    @Override
    public String[] getTypes() {
        return new String[] { InstanceConstants.TYPE };
    }

    @Override
    public void process(Object obj, String type, Map<String, Object> data) {
        if (!(obj instanceof Instance)) {
            return;
        }

        boolean setCreds = false;
        Instance instance = (Instance) obj;
        Object value = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS).get(SystemLabels.LABEL_AGENT_ROLE);
        if (AgentConstants.SYSTEM_ROLE.equals(value)) {
            Account account = objectManager.loadResource(Account.class, instance.getAccountId());
            if (DataAccessor.fieldBool(account, AccountConstants.FIELD_ALLOW_SYSTEM_ROLE)) {
                setCreds = true;
            }
        }

        if (!setCreds) {
            return;
        }

        Credential cred = serviceAccount.getCredential();
        if (cred == null) {
            log.error("Failed to find credential for service account");
            return;
        }

        Map<String, Object> fields = DataUtils.getWritableFields(data);
        DataAccessor.fromMap(fields)
            .withScopeKey(InstanceConstants.FIELD_ENVIRONMENT)
            .withKey("CATTLE_ACCESS_KEY").set(cred.getPublicValue());
        DataAccessor.fromMap(fields)
            .withScopeKey(InstanceConstants.FIELD_ENVIRONMENT)
            .withKey("CATTLE_SECRET_KEY").set(cred.getSecretValue());
        DataAccessor.fromMap(fields)
            .withScopeKey(InstanceConstants.FIELD_ENVIRONMENT)
            .withKey("CATTLE_URL").set(ServerContext.getHostApiBaseUrl(BaseProtocol.HTTP));
    }

}
