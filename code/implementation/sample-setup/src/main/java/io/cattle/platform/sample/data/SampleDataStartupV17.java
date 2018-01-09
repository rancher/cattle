package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.addon.LbConfig;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class SampleDataStartupV17 extends AbstractSampleData {
    @Inject
    ObjectManager objectManager;

    @Override
    protected String getName() {
        return "sampleDataVersion17";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        updateLBServices();
    }

    protected void updateLBServices() {
        List<? extends Service> services = objectManager.find(Service.class,
                SERVICE.KIND, ServiceConstants.KIND_LOAD_BALANCER_SERVICE, SERVICE.REMOVED, null);
        for (Service service: services) {
            LbConfig lbConfig = DataAccessor.field(service, ServiceConstants.FIELD_LB_CONFIG,
                    jsonMapper, LbConfig.class);
            if (lbConfig != null && lbConfig.getPortRules() != null) {
                boolean update = false;
                List<PortRule> rules = new ArrayList<>(); 
                for (PortRule rule : lbConfig.getPortRules()) {
                    if (rule.getWeight() == null) {
                        rules.add(rule);
                    } else {
                        rule.setWeight(null);
                        rules.add(rule);
                        update = true;
                    }
                }
                if (update) {
                    lbConfig.setPortRules(rules);
                    Map<String, Object> params = new HashMap<>();
                    params.put(ServiceConstants.FIELD_LB_CONFIG, lbConfig);
                    objectManager.setFields(service, params);
                }
            }
        }
    }
}
