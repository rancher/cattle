package io.cattle.platform.iaas.api.host;

import io.cattle.platform.api.link.LinkHandler;
import io.cattle.platform.core.constants.HostTemplateConstants;
import io.cattle.platform.core.model.HostTemplate;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

public class HostTemplateLinkHandler implements LinkHandler {

    @Inject
    ObjectManager objectManager;
    @Inject
    SecretsService secretService;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public String[] getTypes() {
        return new String[] { HostTemplateConstants.KIND };
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