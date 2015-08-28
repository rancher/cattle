package io.cattle.iaas.lb.process;

import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerCertificateMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;
import static io.cattle.platform.core.model.tables.LoadBalancerTable.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class LoadBalancerCertificateRemovePostHandler extends AbstractObjectProcessLogic implements ProcessPreListener,
        Priority {

    @Inject
    GenericMapDao mapDao;
    @Inject
    LoadBalancerService lbService;

    @Override
    public String[] getProcessNames() {
        return new String[] { "certificate.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Certificate cert = (Certificate) state.getResource();
        List<? extends LoadBalancerCertificateMap> mapsToRemove = mapDao.findNonRemoved(
                LoadBalancerCertificateMap.class,
                Certificate.class, cert.getId());
        for (LoadBalancerCertificateMap mapToRemove : mapsToRemove) {
            LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.ID,
                    mapToRemove.getLoadBalancerId(), LOAD_BALANCER.REMOVED, null);
            if (lb == null) {
                continue;
            }
            List<? extends LoadBalancerCertificateMap> lbCerts = mapDao.findNonRemoved(
                    LoadBalancerCertificateMap.class,
                    LoadBalancer.class, lb.getId());
            Long defaultCertId = null;
            List<Long> certIds = new ArrayList<>();
            for (LoadBalancerCertificateMap lbCert : lbCerts) {
                if (!lbCert.getId().equals(mapToRemove.getId())) {
                    certIds.add(lbCert.getCertificateId());
                    if (lbCert.getIsDefault()) {
                        defaultCertId = lbCert.getCertificateId();
                    }
                }
            }
            Map<String, Object> data = new HashMap<>();
            data.put(LoadBalancerConstants.FIELD_LB_CERTIFICATE_IDS, certIds);
            data.put(LoadBalancerConstants.FIELD_LB_DEFAULT_CERTIFICATE_ID, defaultCertId);
            DataUtils.getWritableFields(lb).putAll(data);
            objectManager.persist(lb);
            if (lb.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE)
                    || lb.getState().equalsIgnoreCase(CommonStatesConstants.UPDATING_ACTIVE)) {
                objectProcessManager.scheduleStandardProcessAsync(StandardProcess.UPDATE, lb, data);
            }
        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
