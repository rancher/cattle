package io.cattle.platform.iaas.api.host;

import io.cattle.platform.core.constants.HostTemplateConstants;
import io.cattle.platform.core.model.HostTemplate;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

public class HostTemplateOutputFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (original instanceof HostTemplate) {
            HostTemplate ht = (HostTemplate) original;
            Object secretValues = converted.getFields().get(HostTemplateConstants.FIELD_SECRET_VALUES);
            if (secretValues == null || secretValues instanceof String) {
                converted.getFields().put(HostTemplateConstants.FIELD_SECRET_VALUES,
                        DataAccessor.fieldMap(ht, HostTemplateConstants.FIELD_SECRET_VALUES_EMPTY));
            }

            if (!request.getSchemaFactory().getSchema(HostTemplate.class)
                .getResourceFields().get(HostTemplateConstants.FIELD_SECRET_VALUES).isReadOnCreateOnly()) {
                converted.getLinks().put("secretValues", ApiContext.getUrlBuilder().resourceLink(converted, "secretvalues"));
            }
        }

        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[] { HostTemplateConstants.KIND };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{HostTemplate.class};
    }

}
