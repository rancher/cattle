package io.cattle.platform.compose.export.impl;

import com.nimbusds.jose.util.StandardCharset;
import io.cattle.platform.compose.export.ComposeExportService;
import io.cattle.platform.json.JsonMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;

public class ComposeExportServiceImpl implements ComposeExportService {

    private static final String COMPOSINATOR_URL = "http://127.0.0.1:8099/convert";

    JsonMapper jsonMapper;

    public ComposeExportServiceImpl(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public String buildComposeConfig(String stackId) throws IOException {
        Map<String, Object> input = new HashMap<>();
        input.put("stackId", stackId);
        input.put("format", "combined");

        return Request.Post(COMPOSINATOR_URL)
                .bodyString(jsonMapper.writeValueAsString(input), ContentType.APPLICATION_JSON)
                .execute().handleResponse(response -> {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode >= 300) {
                        throw new IOException("Failed to rewrap secret :" + response.getStatusLine().getReasonPhrase());
                    }
                    String result = IOUtils.toString(response.getEntity().getContent(), StandardCharset.UTF_8);
                    Map<String, Object> map = jsonMapper.readValue(result);
                    return map.get("compose").toString();
                });
    }

    @Override
    public Map.Entry<String, String> buildLegacyComposeConfig(String stackId) throws IOException {
        Map<String, Object> input = new HashMap<>();
        input.put("stackId", stackId);
        input.put("format", "split");

        return Request.Post(COMPOSINATOR_URL)
                .bodyString(jsonMapper.writeValueAsString(input), ContentType.APPLICATION_JSON)
                .execute().handleResponse(response -> {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode >= 300) {
                        throw new IOException("Failed to rewrap secret :" + response.getStatusLine().getReasonPhrase());
                    }
                    String result = IOUtils.toString(response.getEntity().getContent(), StandardCharset.UTF_8);
                    Map<String, Object> map = jsonMapper.readValue(result);
                    String dockerCompose = map.get("dockerCompose").toString();
                    String rancherCompose = map.get("rancherCompose").toString();
                    return new SimpleEntry<>(dockerCompose, rancherCompose);
                });
    }

}
