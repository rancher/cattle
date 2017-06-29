package io.cattle.platform.iaas.api.auditing;

import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import java.util.List;
import java.util.Map;

public class ResourceIdInputFilter extends AbstractValidationFilter {

    IdFormatter idFormatter;


    public ResourceIdInputFilter(IdFormatter idFormatter) {
        this.idFormatter = idFormatter;
    }

    @Override
    public Object list(String type, ApiRequest request, ResourceManager next) {
            if (request.getConditions().containsKey(ResourceIdOutputFilter.RESOURCE_ID)) {
                Map<String, List<Condition>> conditions = request
                        .getConditions();
                List<Condition> conditionId = conditions.get(ResourceIdOutputFilter.RESOURCE_ID);

                for (int i = 0; i < conditionId.size(); i++) {
                    conditionId.set(i, new Condition(conditionId.get(i).getConditionType(),
                            idFormatter.parseId((String) conditionId.get(i).getValue())));
                }
            }
        return super.list(type, request, next);
    }
}
