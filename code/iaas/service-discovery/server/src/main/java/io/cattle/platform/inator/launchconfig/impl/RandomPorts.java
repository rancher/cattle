package io.cattle.platform.inator.launchconfig.impl;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.unit.PortUnit;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RandomPorts {

    Map<String, Object> lc;
    InatorServices svc;
    String name;

    public RandomPorts(String name, Map<String, Object> lc, InatorServices svc) {
        this.lc = lc;
        this.svc = svc;
        this.name = name;
    }

    public Map<UnitRef, Unit> getPorts() {
        Map<UnitRef, Unit> result = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<String> ports = (List<String>) CollectionUtils.toList(lc.get(InstanceConstants.FIELD_PORTS));
        for (String port : ports) {
            try {
                PortSpec spec = new PortSpec(port);
                if (spec.getPublicPort() == null) {
                    PortUnit unit = new PortUnit(name, spec.getPrivatePort(), svc);
                    result.put(unit.getRef(), unit);
                }
            } catch (ClientVisibleException e) {
            }
        }

        return result;
    }

}
