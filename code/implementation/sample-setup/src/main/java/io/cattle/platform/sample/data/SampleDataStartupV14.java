package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleDataStartupV14 extends AbstractSampleData {

    @Override
    protected String getName() {
        return "sampleDataVersion14";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        List<Service> services = objectManager
                .find(Service.class, SERVICE.REMOVED, new Condition(ConditionType.NULL));
        for (Service service : services) {
            if (DataAccessor.field(service,
                    ServiceConstants.FIELD_SCALE_POLICY, Object.class) == null) {
                continue;
            }
            Integer lockedScale = DataAccessor.fieldInteger(service,
                    ServiceConstants.FIELD_LOCKED_SCALE);
            Map<String, Object> data = new HashMap<>();
            data.put(ServiceConstants.FIELD_SCALE, lockedScale);
            objectManager.setFields(service, data);
        }
    }

}
