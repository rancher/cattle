package io.cattle.platform.iaas.api.auth.integration.local;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicStringProperty;

public class LocalAuthPasswordValidator {

    public static final DynamicStringProperty AUTH_VALIDATE_URL = ArchaiusUtil.getString("api.auth.local.validate.url");
    public static final DynamicIntProperty AUTH_VALIDATE_TIMEOUT = ArchaiusUtil.getInt("api.auth.local.validate.timeout.milliseconds");

    final static Logger log = LoggerFactory.getLogger(LocalAuthPasswordValidator.class);

    public static void validatePassword(String password, JsonMapper jsonMapper) {
        String authValidateUrl = AUTH_VALIDATE_URL.get();
        if (StringUtils.isBlank(authValidateUrl)) {
            return;
        }

        Map<String, String> data = new HashMap<String, String>();
        data.put("secret", password);
        String jsonString = "";
        Integer code;
        HttpResponse response = null;

        try {
            jsonString = jsonMapper.writeValueAsString(data);
        } catch (IOException e) {
            log.error("Error in creating json for POST request", e);
        }

        try {
            int timeout = AUTH_VALIDATE_TIMEOUT.get();
            Request request = Request.Post(authValidateUrl).bodyString(jsonString, ContentType.APPLICATION_JSON);
            response =  request.connectTimeout(timeout).socketTimeout(timeout).execute().returnResponse();
        } catch (IOException e) {
            log.error("Error sending POST request", e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "Error sending POST request");
        }

        code = response.getStatusLine().getStatusCode();
        if (code >=400 && code <= 499) {
            Map<String, Object> jsonData = new HashMap<String, Object>();
            try {
                jsonData = jsonMapper.readValue(response.getEntity().getContent());
            } catch (IOException e) {
                log.error("No JSON response from validator", e);
            }

            if (!jsonData.containsKey("type") || !jsonData.containsKey("message")) {
                throw new ClientVisibleException(code, "Incomplete JSON response");
            }

            if (jsonData.get("type") != null) {
                throw new ClientVisibleException(code, (String) jsonData.get("message"));
            }
        } else if (code < 200 || code > 299) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "Error talking to validator");
        }
    }
}