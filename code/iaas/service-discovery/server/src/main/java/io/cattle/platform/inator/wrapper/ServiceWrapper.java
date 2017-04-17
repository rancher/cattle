package io.cattle.platform.inator.wrapper;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.util.StateUtil;
import io.cattle.platform.object.util.DataAccessor;

public class ServiceWrapper {

    Service service;

    public ServiceWrapper(Service service) {
        this.service = service;
    }

    public Inator.DesiredState getDesiredState() {
        return StateUtil.getDesiredState(service.getState(), service.getRemoved());
    }

    public Service getService() {
        return service;
    }

    public int getScale() {
        Integer scale = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_SCALE);
        if (scale == null) {
            scale = 0;
        }

        Integer min = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_SCALE_MIN);
        if (min != null && scale < min) {
            scale = min;
        }

        Integer max = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_SCALE_MAX);
        if (max != null && scale > max) {
            scale = max;
        }

        Integer incr = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_SCALE_INCREMENT);
        if (incr != null && (scale%incr) != 0) {
            scale = scale - (scale%incr);
        }

        return scale;
    }

}
