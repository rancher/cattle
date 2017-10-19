package io.cattle.platform.loadbalancer.impl;

import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.addon.LbConfig;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.loadbalancer.LoadBalancerService;
import io.cattle.platform.loadbalancer.lock.LoadBalancerServiceLock;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.List;
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
        reconcileLoadBalancers(service.getAccountId(), (rule, balancer) -> {
            if (service.getId().equals(rule.getServiceId())) {
                return null;
            }
            return rule;
        });
    }

    @Override
    public void registerToLoadBalanceSevices(Instance instance) {
        if (instance.getDeploymentUnitId() == null) {
            return;
        }
        if (instance.getServiceId() != null) {
            return;
        }
        String lcName = DataAccessor.fieldString(instance, InstanceConstants.FIELD_LAUNCH_CONFIG_NAME);
        if (!ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME.equals(lcName)) {
            return;
        }

        reconcileLoadBalancers(instance.getAccountId(), (rule, balancer) -> {
            boolean sameUnit = instance.getDeploymentUnitId().equals(rule.getDeploymentUnitId());
            boolean sameInstance = instance.getId().equals(rule.getInstanceId());

            if (sameUnit && !sameInstance) {
                PortRule newRule = new PortRule(rule);
                newRule.setInstanceId(instance.getId());
                return newRule;
            }
            return rule;
        });
    }

    @Override
    public void removeFromLoadBalancerServices(DeploymentUnit unit) {
        if (unit.getServiceId() != null) {
            return;
        }
        reconcileLoadBalancers(unit.getAccountId(), (rule, balancer) -> {
            if (unit.getId().equals(rule.getDeploymentUnitId())) {
                return null;
            }
            return rule;
        });
    }

    protected void reconcileLoadBalancers(long accountId, BiFunction<PortRule, Service, PortRule> fun) {
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
                    } else if (!newRule.equals(rule)) {
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
