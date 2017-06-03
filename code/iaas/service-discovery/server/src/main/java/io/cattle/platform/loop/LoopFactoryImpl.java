package io.cattle.platform.loop;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.manager.LoopFactory;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.inator.Deployinator;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class LoopFactoryImpl implements LoopFactory {

    public static final String RECONCILE = "service-reconcile";
    public static final String DU_RECONCILE = "deployment-unit-reconcile";

    @Inject
    Deployinator deployinator;

    @Override
    public Loop buildLoop(String name, String type, Long id) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(type) || id == null) {
            return null;
        }

        switch (name) {
        case RECONCILE:
            return new ReconcileLoop(deployinator, Service.class, id);
        case DU_RECONCILE:
            return new ReconcileLoop(deployinator, DeploymentUnit.class, id);
        default:
            break;
        }

        return null;
    }

}
