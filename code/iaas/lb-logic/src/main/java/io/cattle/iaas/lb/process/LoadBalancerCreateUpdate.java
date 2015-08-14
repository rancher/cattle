package io.cattle.iaas.lb.process;

import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.core.addon.LoadBalancerCertificate;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerCreateUpdate extends AbstractObjectProcessHandler {
    @Inject
    LoadBalancerService lbService;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public String[] getProcessNames() {
        return new String[] { LoadBalancerConstants.PROCESS_LB_CREATE, LoadBalancerConstants.PROCESS_LB_UPDATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        LoadBalancer lb = (LoadBalancer) state.getResource();

        Set<LoadBalancerCertificate> newCertsSet = new HashSet<>();
        List<? extends Long> newCerts = DataAccessor.fromMap(state.getData())
                .withKey(LoadBalancerConstants.FIELD_LB_CERTIFICATE_IDS).withDefault(Collections.EMPTY_LIST)
                .asList(jsonMapper, Long.class);
        for (Long newCertId : newCerts) {
            newCertsSet.add(new LoadBalancerCertificate(newCertId, false));
        }
        Long defaultCert = DataAccessor.fromMap(state.getData())
                .withKey(LoadBalancerConstants.FIELD_LB_DEFAULT_CERTIFICATE_ID).as(Long.class);
        if (defaultCert != null) {
            newCertsSet.add(new LoadBalancerCertificate(defaultCert, true));
        }
        
        lbService.updateLoadBalancerCertificates(lb, newCertsSet);

        return null;
    }
}
