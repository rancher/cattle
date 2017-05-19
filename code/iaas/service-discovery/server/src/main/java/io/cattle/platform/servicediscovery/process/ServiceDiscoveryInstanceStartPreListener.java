package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.InstanceTable.*;

import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class ServiceDiscoveryInstanceStartPreListener extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Inject
    ServiceDiscoveryService sdService;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        addToLoadBalancer(state, process);

        return null;
    }

    protected void addToLoadBalancer(ProcessState state, ProcessInstance process) {
        final Instance instance = (Instance)state.getResource();

        if (instance.getServiceId() != null || instance.getDeploymentUnitId() == null || instance.getFirstRunning() != null) {
            return;
        }

        String lcName = DataAccessor.fieldString(instance, InstanceConstants.FIELD_LAUNCH_CONFIG_NAME);
        if (StringUtils.isBlank(lcName)) {
            return;
        }
        Instance replacement = null;
        Date removed = null;
        List<Instance> instances = objectManager.find(Instance.class,
                INSTANCE.REMOVED, new Condition(ConditionType.NOTNULL, (Object)null),
                INSTANCE.DESIRED, false,
                INSTANCE.DEPLOYMENT_UNIT_ID, instance.getDeploymentUnitId());

        for (Instance oldInstance : instances) {
            if (!lcName.equals(DataAccessor.fieldString(instance, InstanceConstants.FIELD_LAUNCH_CONFIG_NAME))) {
                continue;
            }

            if (removed == null || oldInstance.getRemoved().after(removed)) {
                replacement = oldInstance;
                removed = oldInstance.getRemoved();
            }
        }

        if (replacement == null) {
            return;
        }

        Map<String, Object> portRules = DataAccessor.fieldMapRO(replacement, InstanceConstants.FIELD_LB_RULES_ON_REMOVE);
        for (Map.Entry<String, Object> entry : portRules.entrySet()) {
            Long id = new Long(entry.getKey().toString());
            List<PortRule> rules = jsonMapper.convertCollectionValue(entry.getValue(), List.class, PortRule.class);
            for (PortRule rule : rules) {
                rule.setInstanceId(instance.getId().toString());
            }
            sdService.addToBalancerService(id, rules);
        }
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.start" };
    }
}
