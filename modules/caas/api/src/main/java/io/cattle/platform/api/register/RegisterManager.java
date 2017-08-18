package io.cattle.platform.api.register;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.resource.AbstractNoOpResourceManager;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.addon.K8sClientConfig;
import io.cattle.platform.core.addon.Register;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Cluster;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;

public class RegisterManager extends AbstractNoOpResourceManager {

    AgentDao agentDao;
    ObjectManager objectManager;
    ObjectProcessManager processManager;

    public RegisterManager(AgentDao agentDao, ObjectManager objectManager, ObjectProcessManager processManager) {
        this.agentDao = agentDao;
        this.objectManager = objectManager;
        this.processManager = processManager;
    }

    @Override
    public Object create(String type, ApiRequest request) {
        Long clusterId = ApiUtils.getPolicy().getClusterId();
        Register register = request.proxyRequestObject(Register.class);
        if (clusterId == null) {
            return null;
        }

        K8sClientConfig config = register.getK8sClientConfig();
        if (config == null) {
            createAgent(register, clusterId);
        }

        createOrUpdateCluster(config, clusterId);
        return register;
    }

    private void createOrUpdateCluster(K8sClientConfig clientConfig, long clusterId) {
        Cluster cluster = objectManager.loadResource(Cluster.class, clusterId);
        Object existingConfig = DataAccessor.field(cluster, ClusterConstants.FIELD_K8S_CLIENT_CONFIG, Object.class);

        if (CommonStatesConstants.INACTIVE.equals(cluster.getState())) {
            objectManager.setFields(cluster,
                 ClusterConstants.FIELD_K8S_CLIENT_CONFIG, clientConfig);
            processManager.activate(cluster, null);
        } else if (CommonStatesConstants.ACTIVE.equals(cluster.getState()) && existingConfig != null) {
            objectManager.setFields(cluster,
                    ClusterConstants.FIELD_K8S_CLIENT_CONFIG, clientConfig);
            processManager.update(cluster, null);
        }
    }

    private Register createAgent(Register register, long clusterId) {
        if (StringUtils.isBlank(register.getKey())) {
            return null;
        }

        Cluster cluster = objectManager.loadResource(Cluster.class, clusterId);

        try {
            ApiUtils.getPolicy().setOption(Policy.OVERRIDE_ACCOUNT_ID, "true");
            agentDao.createAgentForRegistration(register, clusterId);
            register.setState(CommonStatesConstants.CREATING);
            register.setId(register.getKey());

            if (CommonStatesConstants.INACTIVE.equals(cluster.getState())) {
                processManager.activate(cluster, null);
            }

            return register;
        } finally {
            ApiUtils.getPolicy().setOption(Policy.OVERRIDE_ACCOUNT_ID, "false");
        }
    }


    @Override
    public Object listSupport(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        Long clusterId = ApiUtils.getPolicy().getClusterId();
        String key = RequestUtils.getConditionValue("key", criteria);
        if (StringUtils.isBlank(key)) {
            key = RequestUtils.getConditionValue(ObjectMetaDataManager.ID_FIELD, criteria);
        }
        if (clusterId == null || StringUtils.isBlank(key)) {
            return Collections.emptyList();
        }

        Agent agent = agentDao.findAgentByExternalId(key, clusterId);
        if (agent == null) {
            return Collections.emptyList();
        }

        Credential cred = agentDao.findAgentCredentailByExternalId(key, clusterId);
        return Collections.singletonList(new Register(key, cred));
    }

}
