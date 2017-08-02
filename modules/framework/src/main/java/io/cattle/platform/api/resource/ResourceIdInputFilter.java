package io.cattle.platform.api.resource;

import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import java.util.List;

public class ResourceIdInputFilter extends AbstractValidationFilter {

    IdFormatter idFormatter;

    public ResourceIdInputFilter(IdFormatter idFormatter) {
        this.idFormatter = idFormatter;
    }

    @Override
    public Object list(String type, ApiRequest request, ResourceManager next) {
        replaceWithObfuscatedValue(request, ResourceIdOutputFilter.RESOURCE_ID);
        return super.list(type, request, next);
    }

    private void replaceWithObfuscatedValue(ApiRequest request, String field) {
        List<Condition> conditions = request.getConditions().get(field);
        if (conditions == null) {
            return;
        }

        for (Condition condition : conditions) {
            Object value = condition.getValue();
            if (value instanceof String) {
                condition.setValue(idFormatter.parseId((String)value));
            }
        }
    }

}
