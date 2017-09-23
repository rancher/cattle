package io.cattle.platform.process.instance;

import io.cattle.platform.agent.util.AgentUtils;
import io.cattle.platform.core.addon.DeploymentSyncRequest;
import io.cattle.platform.core.addon.DeploymentSyncResponse;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.service.launcher.ServiceAccountCreateStartup;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static io.cattle.platform.core.model.Tables.*;

public class DeploymentSyncFactory {

    InstanceDao instanceDao;
    VolumeDao volumeDao;
    NetworkDao networkDao;
    ObjectManager objectManager;
    ServiceAccountCreateStartup serviceAccount;
    JsonMapper jsonMapper;

    public DeploymentSyncFactory(InstanceDao instanceDao, VolumeDao volumeDao, NetworkDao networkDao, ObjectManager objectManager, ServiceAccountCreateStartup serviceAccount,
                                 JsonMapper jsonMapper) {
        this.instanceDao = instanceDao;
        this.volumeDao = volumeDao;
        this.objectManager = objectManager;
        this.serviceAccount = serviceAccount;
        this.jsonMapper = jsonMapper;
        this.networkDao = networkDao;
    }

    public DeploymentSyncResponse getResponse(Map<Object, Object> data) {
        return jsonMapper.convertValue(DataAccessor.fromMap(data).withKey("deploymentSyncResponse").get(),
                DeploymentSyncResponse.class);
    }

    public DeploymentSyncRequest construct(DeploymentUnit unit) {
        Account account = objectManager.loadResource(Account.class, unit.getAccountId());
        
        return new DeploymentSyncRequest(unit,
                null,
                StringUtils.isBlank(account.getExternalId()) ? account.getName().toLowerCase() : account.getExternalId(),
                        getRevision(unit, new TreeMap<>()),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(), account.getClusterId());
    }

    public DeploymentSyncRequest construct(Instance resource) {
        List<Instance> instances = new ArrayList<>();
        Map<Long, Instance> instanceById = new TreeMap<>();
        Set<Long> credentialIds = new HashSet<>();
        Set<Long> volumeIds = new HashSet<>();
        Set<Long> networkIds = new HashSet<>();

        instances.add(resource);
        instances.addAll(instanceDao.getOtherDeploymentInstances(resource));

        for (Instance instance : instances) {
            instanceById.put(instance.getId(), instance);

            if (instance.getRegistryCredentialId() != null) {
                credentialIds.add(instance.getRegistryCredentialId());
            }

            Map<String, Object> dataVolumesMounts = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_DATA_VOLUME_MOUNTS);
            for (Object idObj : dataVolumesMounts.values()) {
                if (idObj instanceof Number) {
                    volumeIds.add(((Number)idObj).longValue());
                }
            }

            networkIds.addAll(DataAccessor.fieldLongList(instance, InstanceConstants.FIELD_NETWORK_IDS));

            addRoleAccounts(instance);
        }

        List<Volume> volumes = new ArrayList<>();
        if (volumeIds.size() > 0) {
            volumes.addAll(volumeDao.getVolumes(volumeIds));
        }

        List<Credential> credentials = new ArrayList<>();
        if (credentialIds.size() > 0) {
            List<Credential> creds = objectManager.find(Credential.class,
                    CREDENTIAL.ID, Condition.in(credentialIds));
            credentials.addAll(creds);
        }

        List<Network> networks = new ArrayList<>();
        if (networkIds.size() > 0) {
            networks.addAll(networkDao.getNetworks(networkIds));
        }

        DeploymentUnit unit = objectManager.loadResource(DeploymentUnit.class, resource.getDeploymentUnitId());
        Account account = objectManager.loadResource(Account.class, resource.getAccountId());
        Host host = objectManager.loadResource(Host.class, resource.getHostId() == null ?
                DataAccessor.fieldLong(resource, InstanceConstants.FIELD_REQUESTED_HOST_ID) : resource.getHostId());

        return new DeploymentSyncRequest(unit,
                host == null ? null : DataAccessor.fieldString(host, HostConstants.FIELD_NODE_NAME),
                StringUtils.isBlank(account.getExternalId()) ? account.getName().toLowerCase() : account.getExternalId(),
                getRevision(unit, instanceById),
                instances,
                volumes,
                credentials,
                networks, account.getClusterId());
    }

    private String getRevision(DeploymentUnit unit, Map<Long, Instance> instances) {
        MessageDigest digest;

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
                digest.update(instance.getUuid().getBytes());
            }
        }

        return Hex.encodeHexString(digest.digest());
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
        if (auth == null) {
            return;
        }

        Map<String, Object> fields = DataAccessor.getWritableFields(instance);
        for (Map.Entry<String, Object> entry : auth.entrySet()) {
            DataAccessor.fromMap(fields)
                    .withScopeKey(InstanceConstants.FIELD_ENVIRONMENT)
                    .withKey(entry.getKey())
                    .set(entry.getValue());
        }
        DataAccessor.fromMap(fields)
                .withScopeKey(InstanceConstants.FIELD_ENVIRONMENT)
                .withKey("CATTLE_URL")
                .set(ServerContext.getHostApiBaseUrl(ServerContext.BaseProtocol.HTTP));
    }
}
