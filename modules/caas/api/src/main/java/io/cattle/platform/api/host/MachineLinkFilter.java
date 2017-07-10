package io.cattle.platform.api.host;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;
import org.apache.commons.lang3.StringUtils;

import static io.cattle.platform.core.constants.HostConstants.*;

public class MachineLinkFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        boolean add = false;
        if (original instanceof Host) {
            if (StringUtils.isNotEmpty((String) DataUtils.getFields(original).get(EXTRACTED_CONFIG_FIELD))) {
                add = canAccessConfig();
            }
            if (!add && original instanceof Host && StringUtils.isNotEmpty((String) converted.getFields().get(FIELD_DRIVER))) {
                add = canAccessConfig();
            }
        }

        if (add) {
            converted.getLinks().put(CONFIG_LINK, ApiContext.getUrlBuilder().resourceLink(converted, CONFIG_LINK));
        }

        return converted;
    }

    public static boolean canAccessConfig() {
        SchemaFactory schemaFactory = ApiContext.getSchemaFactory();
        Schema machineSchema = schemaFactory == null ? null : schemaFactory.getSchema(Host.class);
        return machineSchema != null && machineSchema.getCollectionMethods().contains(Schema.Method.POST.toString());
    }

}