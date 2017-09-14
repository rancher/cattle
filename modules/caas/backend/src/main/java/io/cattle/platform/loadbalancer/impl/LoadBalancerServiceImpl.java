package io.cattle.platform.loadbalancer.impl;

import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.addon.LbConfig;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.loadbalancer.LoadBalancerService;
import io.cattle.platform.loadbalancer.lock.LoadBalancerServiceLock;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class LoadBalancerServiceImpl implements LoadBalancerService {

    JsonMapper jsonMapper;
    LockManager lockManager;
    ObjectManager objectManager;

    public LoadBalancerServiceImpl(JsonMapper jsonMapper, LockManager lockManager, ObjectManager objectManager) {
        super();
        this.jsonMapper = jsonMapper;
        this.lockManager = lockManager;
        this.objectManager = objectManager;
    }

    @Override
    public void removeFromLoadBalancerServices(Service service) {
        removeFromLoadBalancer(service.getAccountId(), (rule, balancer) -> {
            if (service.getId().equals(rule.getServiceId())) {
                return null;
            }
            return rule;
        });
    }

    @Override
    public void removeFromLoadBalancerServices(Instance instance) {
        removeFromLoadBalancer(instance.getAccountId(), (rule, balancer) -> {
            if (instance.getId().equals(rule.getInstanceId())) {
                if (instance.getServiceId() == null) {
                    Map<String, Object> portRules = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LB_RULES_ON_REMOVE);
                    PortRule newRule = new PortRule(rule);
                    newRule.setInstanceId(null);

                    Object list = portRules.get(balancer.getId().toString());
                    if (list == null) {
                        list = Arrays.asList(newRule);
                    } else {
                        List<PortRule> ruleList = jsonMapper.convertCollectionValue(list, List.class, PortRule.class);
                        if (!ruleList.contains(newRule)) {
                            ruleList.add(newRule);
                        }
                        list = ruleList;
                    }
                    portRules.put(balancer.getId().toString(), list);
                    objectManager.setFields(instance,
                            InstanceConstants.FIELD_LB_RULES_ON_REMOVE, portRules);
                }
                return null;
            }
            return rule;
        });
    }

    protected void removeFromLoadBalancer(long accountId, BiFunction<PortRule, Service, PortRule> fun) {
        List<? extends Service> balancers = objectManager.find(Service.class,
                SERVICE.KIND, ServiceConstants.KIND_LOAD_BALANCER_SERVICE,
                SERVICE.REMOVED, null,
                SERVICE.ACCOUNT_ID, accountId);
        for (Service i : balancers) {
            lockManager.lock(new LoadBalancerServiceLock(i.getId()), () -> {
                Service balancer = objectManager.loadResource(Service.class, i.getId());
                LbConfig lbConfig = DataAccessor.field(balancer, ServiceConstants.FIELD_LB_CONFIG, LbConfig.class);
                if (lbConfig.getPortRules() == null) {
                    return null;
                }

                boolean changed = false;
                List<PortRule> newRules = new ArrayList<>();
                for (PortRule rule : lbConfig.getPortRules()) {
                    PortRule newRule = fun.apply(rule, balancer);
                    if (newRule == null) {
                        changed = true;
                    } else if (newRule != rule) {
                        newRules.add(newRule);
                        changed = true;
                    } else {
                        newRules.add(newRule);
                    }
                }

                if (changed) {
                    lbConfig.setPortRules(newRules);
                    objectManager.setFields(balancer, ServiceConstants.FIELD_LB_CONFIG, lbConfig);
                }

                return null;
            });
        }
    }


}
