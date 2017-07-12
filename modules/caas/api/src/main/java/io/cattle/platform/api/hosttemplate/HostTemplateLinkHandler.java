package io.cattle.platform.api.hosttemplate;

import io.cattle.platform.core.constants.HostTemplateConstants;
import io.cattle.platform.core.model.HostTemplate;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.LinkHandler;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

public class HostTemplateLinkHandler implements LinkHandler {

    SecretsService secretService;

    public HostTemplateLinkHandler(SecretsService secretService) {
        super();
        this.secretService = secretService;
    }

    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return "secretvalues".equalsIgnoreCase(link);
    }

    @Override
    public Object link(String name, Object obj, ApiRequest request) throws IOException {
        if (!(obj instanceof HostTemplate)) {
            return null;
        }

        HostTemplate ht = (HostTemplate)obj;
        Schema s = request.getSchemaFactory().getSchema(HostTemplate.class);
        if (!s.getResourceFields().get(HostTemplateConstants.FIELD_SECRET_VALUES).isReadOnCreateOnly()) {
            String secrets = DataAccessor.fieldString(obj, HostTemplateConstants.FIELD_SECRET_VALUES);
            if (secrets != null) {
                try {
                    secrets = secretService.decrypt(ht.getAccountId(), secrets);
                    request.setResponseContentType("application/json");
                    request.setResponseObject(new Object());
                    IOUtils.write(Base64.decodeBase64(secrets), request.getOutputStream());
                    return new Object();
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        }

        return null;
    }

}