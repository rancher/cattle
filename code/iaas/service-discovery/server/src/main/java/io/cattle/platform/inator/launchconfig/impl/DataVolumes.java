package io.cattle.platform.inator.launchconfig.impl;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.unit.VolumeUnit;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

public class DataVolumes {

    Map<String, Object> lc;
    InatorServices svc;


    public DataVolumes(Map<String, Object> lc, InatorServices svc) {
        super();
        this.lc = lc;
        this.svc = svc;
    }

    public Map<UnitRef, Unit> getVolumes(Map<String, VolumeTemplate> templates) {
        Map<UnitRef, Unit> result = new HashMap<>();

        for (Object volumeMapping : CollectionUtils.toList(lc.get(InstanceConstants.FIELD_DATA_VOLUMES))) {
            String[] parts = volumeMapping.toString().split(":", 2);
            if (!isNamedVolume(parts) || !templates.containsKey(parts[0])) {
                continue;
            }

            VolumeUnit volumeUnit = new VolumeUnit(templates.get(parts[0]),  svc);
            result.put(volumeUnit.getRef(), volumeUnit);
        }

        return result;
    }

    protected boolean isNamedVolume(String[] parts) {
        return parts.length == 2 && parts[0].length() > 0 && parts[0].charAt(0) != '/';
    }

}
