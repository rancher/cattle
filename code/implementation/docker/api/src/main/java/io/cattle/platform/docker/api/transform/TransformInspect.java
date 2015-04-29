package io.cattle.platform.docker.api.transform;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.docker.transform.DockerTransformer;
import io.cattle.platform.iaas.api.request.handler.ScriptsHandler;
import io.cattle.platform.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;

public class TransformInspect implements ScriptsHandler {

    @Inject
    DockerTransformer transformer;

    @Inject
    JsonMapper jsonMapper;

    @SuppressWarnings("unchecked")
    @Override
    public boolean handle(ApiRequest request) throws IOException {
        if (!"transform".equals(request.getId())) {
            return false;
        }

        request.setResponseContentType("application/json");

        Object reqObj = getObject(request);
        if (reqObj == null) {
            return true;
        }

        Instance instance = new InstanceRecord();
        transformer.transform((Map<String, Object>)reqObj, instance);
        request.setResponseObject(instance);
        return true;
    }

    protected Object getObject(ApiRequest request) throws IOException {
        if (!RequestUtils.mayHaveBody(request.getMethod())) {
            return null;
        }

        InputStream is = request.getInputStream();
        if (is == null) {
            return null;
        }

        byte[] content = IOUtils.toByteArray(is);
        if (content.length == 0) {
            return null;
        }

        try {
            return jsonMapper.readValue(content);
        } catch (IOException e) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, ValidationErrorCodes.INVALID_BODY_CONTENT);
        }
    }
}
