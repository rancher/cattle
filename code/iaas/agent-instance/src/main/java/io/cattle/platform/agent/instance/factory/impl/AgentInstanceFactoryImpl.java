package io.cattle.platform.agent.instance.factory.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;

import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.docker.client.DockerImage;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.netflix.config.DynamicStringProperty;

public class AgentInstanceFactoryImpl implements AgentInstanceFactory {
    private static final DynamicStringProperty LB_IMAGE_UUID = ArchaiusUtil.getString("lb.instance.image.uuid");

    ObjectManager objectManager;
    AgentDao agentDao;
    GenericResourceDao resourceDao;
    ResourceMonitor resourceMonitor;
    ObjectProcessManager processManager;

    public AgentInstanceFactoryImpl(ObjectManager objectManager, AgentDao agentDao, GenericResourceDao resourceDao, ResourceMonitor resourceMonitor,
            ObjectProcessManager processManager) {
        super();
        this.objectManager = objectManager;
        this.agentDao = agentDao;
        this.resourceDao = resourceDao;
        this.resourceMonitor = resourceMonitor;
        this.processManager = processManager;
    }

    @Override
    public Agent createAgent(Instance instance) {
        if (shouldCreateAgent(instance)) {
            Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
            Set<String> filteredRoles = new HashSet<>();

            String rolesVal = labels.get(SystemLabels.LABEL_AGENT_ROLE) != null ? labels.get(SystemLabels.LABEL_AGENT_ROLE).toString() : null;
            if (rolesVal != null) {
                String[] roles = rolesVal.split(",");
                for (String r : roles) {
                    if ("environment".equals(r) || "agent".equals(r)) {
                        filteredRoles.add(r);
                    } else if ("environmentAdmin".equals(r) && isSystem(instance)) {
                        filteredRoles.add(r);
                    }
                }
            }

            return getAgent(new AgentBuilderRequest(instance, filteredRoles));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private boolean isLBSystemService(Service service) {
        if (!service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            return false;
        }
        Map<String, Object> data = DataAccessor.fields(service)
                .withKey("launchConfig").withDefault(Collections.EMPTY_MAP)
                .as(Map.class);

        Object imageObj = data.get(InstanceConstants.FIELD_IMAGE_UUID);
        if (imageObj == null) {
            return false;
        }

        Pair<String, String> defaultImage = getImageAndRepo(LB_IMAGE_UUID.get().toLowerCase());
        Pair<String, String> instanceImage = getImageAndRepo(imageObj.toString().toLowerCase());
        return defaultImage.getRight().equalsIgnoreCase(instanceImage.getRight())
                && defaultImage.getLeft().equalsIgnoreCase(instanceImage.getLeft());
    }

    private Pair<String, String> getImageAndRepo(String imageUUID) {
        DockerImage dockerImage = DockerImage.parse(imageUUID);
        String[] splitted = dockerImage.getFullName().split("/");
        if (splitted.length < 2) {
            return Pair.of("", "");
        }
        String repo = splitted[0];
        // split the version
        String image = splitted[1].split(":")[0];
        return Pair.of(repo, image);
    }

    private boolean isSystem(Instance instance) {
        if (instance.getSystem()) {
            return true;
        }

        Service service = objectManager.loadResource(Service.class, instance.getServiceId());
        if (service != null && isLBSystemService(service)) {
            return true;
        }
        return false;
    }

    @Override
    public void deleteAgent(Instance instance) {
        if (!shouldCreateAgent(instance) || instance.getAgentId() == null) {
            return;
        }

        Agent agent = objectManager.loadResource(Agent.class, instance.getAgentId());
        if (agent == null) {
            return;
        }

        processManager.deactivateThenRemove(agent, null);
    }

    protected boolean shouldCreateAgent(Instance instance) {
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        return BooleanUtils.toBoolean(Objects.toString(labels.get(SystemLabels.LABEL_AGENT_CREATE), null));
    }

    protected Agent getAgent(AgentBuilderRequest builder) {
        String uri = builder.getUri();
        Agent agent = agentDao.findNonRemovedByUri(uri);

        if (agent == null) {
            agent = createAgent(uri, builder);
        }

        agent = resourceMonitor.waitFor(agent, new ResourcePredicate<Agent>() {
            @Override
            public boolean evaluate(Agent agent) {
                return agentDao.areAllCredentialsActive(agent);
            }

            @Override
            public String getMessage() {
                return "active credentials";
            }
        });

        return agent;
    }

    protected Agent createAgent(final String uri, final AgentBuilderRequest builder) {
        Agent agent = agentDao.findNonRemovedByUri(uri);
        Map<String, Object> data = new HashMap<>();

        if (builder.getRequestedRoles() != null) {
            data.put(AgentConstants.DATA_REQUESTED_ROLES, new ArrayList<>(builder.getRequestedRoles()));
        }

        if (agent != null) {
            return agent;
        }

        return DeferredUtils.nest(new Callable<Agent>() {
            @Override
            public Agent call() throws Exception {
                return resourceDao.createAndSchedule(Agent.class,
                        AGENT.DATA, data,
                        AGENT.URI, uri,
                        AGENT.RESOURCE_ACCOUNT_ID, builder.getResourceAccountId(),
                        AGENT.MANAGED_CONFIG, false);
            }
        });
    }

}
