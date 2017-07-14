package io.cattle.platform.process.instance;

import io.cattle.platform.agent.util.AgentUtils;
import io.cattle.platform.core.addon.DeploymentSyncRequest;
import io.cattle.platform.core.addon.DeploymentSyncResponse;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.service.launcher.ServiceAccountCreateStartup;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DeploymentSyncFactory {

    private static final Logger log = LoggerFactory.getLogger(DeploymentSyncRequest.class);

    InstanceDao instanceDao;
    VolumeDao volumeDao;
    ObjectManager objectManager;
    ServiceAccountCreateStartup serviceAccount;
    JsonMapper jsonMapper;

    public DeploymentSyncFactory(InstanceDao instanceDao, VolumeDao volumeDao, ObjectManager objectManager, ServiceAccountCreateStartup serviceAccount,
                                 JsonMapper jsonMapper) {
        this.instanceDao = instanceDao;
        this.volumeDao = volumeDao;
        this.objectManager = objectManager;
        this.serviceAccount = serviceAccount;
        this.jsonMapper = jsonMapper;
    }

    public DeploymentSyncResponse getResponse(Event event) {
        return jsonMapper.convertValue(event.getData(), DeploymentSyncResponse.class);
    }

    public DeploymentSyncRequest construct(Instance resource) {
        List<Instance> instances = new ArrayList<>();
        Map<Long, Instance> instanceById = new TreeMap<>();
        Set<Long> credentialIds = new HashSet<>();
        Set<Long> volumeIds = new HashSet<>();

        instances.add(resource);
        instances.addAll(instanceDao.getOtherDeploymentInstances(resource));


        for (Instance instance : instances) {
            if (!CommonStatesConstants.REMOVING.equals(instance.getState())) {
                instanceById.put(instance.getId(), instance);
            }

            if (instance.getRegistryCredentialId() != null) {
                credentialIds.add(instance.getRegistryCredentialId());
            }

            Map<String, Object> dataVolumesMounts = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_DATA_VOLUME_MOUNTS);
            for (Object idObj : dataVolumesMounts.values()) {
                if (idObj instanceof Number) {
                    volumeIds.add(((Number)idObj).longValue());
                }
            }

            addSystemCredentials(instance);
            addRoleAccounts(instance);
        }

        List<Volume> volumes = new ArrayList<>();
        if (volumeIds.size() > 0) {
            volumes.addAll(volumeDao.getVolumes(volumeIds));
        }

        List<Credential> credentials = new ArrayList<>();
        if (credentialIds.size() > 0) {
            credentials.addAll(instanceDao.getCredentials(credentialIds));
        }

        DeploymentUnit unit = objectManager.loadResource(DeploymentUnit.class, resource.getDeploymentUnitId());

        DeploymentSyncRequest request = new DeploymentSyncRequest(unit,
                getRevision(unit, instanceById),
                instances,
                volumes,
                credentials);

        return request;
    }

    private String getRevision(DeploymentUnit unit, Map<Long, Instance> instances) {
        MessageDigest digest = null;

        try {
            // Not used for any security or verification, just need a short string
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }

        if (unit != null) {
            if (unit.getRequestedRevisionId() != null) {
                digest.update(unit.getRequestedRevisionId().toString().getBytes());
            } else if (unit.getRevisionId() != null) {
                digest.update(unit.getRevisionId().toString().getBytes());
            }
        }

        for (Instance instance : instances.values()) {
            if (instance.getVersion() != null) {
                digest.update(instance.getVersion().getBytes());
            }
        }

        return Hex.encodeHexString(digest.digest());
    }

    private void addSystemCredentials(Instance instance) {
        boolean setCreds = false;
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

        Map<String, Object> fields = DataAccessor.getWritableFields(instance);
        DataAccessor.fromMap(fields)
                .withScopeKey(InstanceConstants.FIELD_ENVIRONMENT)
                .withKey("CATTLE_ACCESS_KEY").set(cred.getPublicValue());
        DataAccessor.fromMap(fields)
                .withScopeKey(InstanceConstants.FIELD_ENVIRONMENT)
                .withKey("CATTLE_SECRET_KEY").set(cred.getSecretValue());
        DataAccessor.fromMap(fields)
                .withScopeKey(InstanceConstants.FIELD_ENVIRONMENT)
                .withKey("CATTLE_URL").set(ServerContext.getHostApiBaseUrl(ServerContext.BaseProtocol.HTTP));
    }

    private void addRoleAccounts(Instance instance) {
        Agent agent = objectManager.loadResource(Agent.class, instance.getAgentId());
        if (agent == null) {
            return;
        }

        List<Long> authedRoleAccountIds = DataAccessor.fieldLongList(agent, AgentConstants.FIELD_AUTHORIZED_ROLE_ACCOUNTS);
        if (authedRoleAccountIds.isEmpty()) {
            Map<String, Object> auth = AgentUtils.getAgentAuth(agent, objectManager);
            setAuthEnvVars(instance, auth);
        } else {
            // Primary agent account
            Account account = objectManager.loadResource(Account.class, agent.getAccountId());
            Map<String, Object> auth = AgentUtils.getAccountScopedAuth(account, objectManager, account.getKind());
            setAuthEnvVars(instance, auth);

            // Secondary authed roles
            for (Long accountId : authedRoleAccountIds) {
                account = objectManager.loadResource(Account.class, accountId);
                String scope = null;
                if (DataAccessor.fromDataFieldOf(account).withKey(AccountConstants.DATA_ACT_AS_RESOURCE_ACCOUNT).withDefault(false).as(Boolean.class)) {
                    scope = "ENVIRONMENT";
                } else if (DataAccessor.fromDataFieldOf(account).withKey(AccountConstants.DATA_ACT_AS_RESOURCE_ADMIN_ACCOUNT).withDefault(false)
                        .as(Boolean.class)) {
                    scope = "ENVIRONMENT_ADMIN";
                }
                if (scope != null) {
                    Map<String, Object> secondaryAuth = AgentUtils.getAccountScopedAuth(account, objectManager, scope);
                    setAuthEnvVars(instance, secondaryAuth);
                }
            }
        }
    }

    void setAuthEnvVars(Instance instance, Map<String, Object> auth) {
        if (auth != null) {
            Map<String, Object> fields = DataAccessor.getWritableFields(instance);
            for (Map.Entry<String, Object> entry : auth.entrySet()) {
                DataAccessor.fromMap(fields)
                        .withScopeKey(InstanceConstants.FIELD_ENVIRONMENT)
                        .withKey(entry.getKey())
                        .set(entry.getValue());
            }
        }
    }
}
