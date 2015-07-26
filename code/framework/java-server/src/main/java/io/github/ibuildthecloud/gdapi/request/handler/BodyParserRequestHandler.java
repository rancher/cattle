package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;

public class BodyParserRequestHandler extends AbstractApiRequestHandler implements ApiRequestHandler {

    JsonMapper jsonMarshaller;
    Set<Class<?>> allowedTypes;

    @Override
    public void handle(ApiRequest request) throws IOException {
        Object obj = getObject(request);
        request.setRequestObject(merge(obj, request));
    }

    protected Object getObject(ApiRequest request) throws IOException {
        if (!RequestUtils.mayHaveBody(request.getMethod()))
            return null;

        if (!shouldBeParsed(request)) {
            return null;
        }

        InputStream is = request.getInputStream();
        if (is == null) {
            return null;
        }

        byte[] content = IOUtils.toByteArray(is);

        if (content.length == 0)
            return null;

        try {
            Object body = jsonMarshaller.readValue(content);

            if (isAllowedType(body)) {
                return body;
            }
        } catch (IOException e) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, ValidationErrorCodes.INVALID_BODY_CONTENT);
        }

        return null;
    }

    protected boolean isAllowedType(Object obj) {
        if (obj == null) {
            return false;
        }

        boolean accepted = false;
        for (Class<?> type : allowedTypes) {
            if (type.isAssignableFrom(obj.getClass())) {
                accepted = true;
                break;
            }
        }

        return accepted;
    }

    protected boolean shouldBeParsed(ApiRequest request) {
        return true;
    }

    protected Object merge(Object body, ApiRequest request) {
        if (body instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>)body;
            return mergeMap(map, request);
        } else if (body instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>)body;
            List<Object> result = new ArrayList<Object>(list.size());
            for (Object object : list) {
                if (isAllowedType(object)) {
                    result.add(merge(object, request));
                }
            }
            return result;
        } else {
            return mergeMap(null, request);
        }
    }

    protected Map<String, Object> mergeMap(Map<String, Object> overlay, ApiRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();

        /*
         * Notice that this loop makes the value singular if it can. This is because the request params are always a String[] from the
         * HttpServletRequest.getParametersMap()
         */
        for (Map.Entry<String, Object> entry : request.getRequestParams().entrySet()) {
            result.put(entry.getKey(), RequestUtils.makeSingularIfCan(entry.getValue()));
        }

        if (overlay != null) {
            for (Map.Entry<String, Object> entry : overlay.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    @PostConstruct
    public void init() {
        if (allowedTypes == null) {
            allowedTypes = new HashSet<Class<?>>();
            allowedTypes.add(Map.class);
            allowedTypes.add(List.class);
        }
    }

    public JsonMapper getJsonMarshaller() {
        return jsonMarshaller;
    }

    @Inject
    public void setJsonMarshaller(JsonMapper jsonMarshaller) {
        this.jsonMarshaller = jsonMarshaller;
    }

    public Set<Class<?>> getAllowedTypes() {
        return allowedTypes;
    }

    public void setAllowedTypes(Set<Class<?>> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }

}
