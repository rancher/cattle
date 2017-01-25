package io.cattle.platform.framework.secret;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.SecretReference;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.dao.SecretDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Secret;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import com.netflix.config.DynamicStringProperty;

public class SecretsServiceImpl implements SecretsService {

    private static final String CREATE_PATH = "/v1-secrets/secrets/create";
    private static final String BULK_PATH = "/v1-secrets/secrets/rewrap?action=bulk";
    private static final DynamicStringProperty SECRETS_URL = ArchaiusUtil.getString("secrets.url");
    private static final DynamicStringProperty SECRETS_BACKEND = ArchaiusUtil.getString("secrets.backend");

    @Inject
    SecretDao secretDao;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public String encrypt(long accountId, String value) throws IOException {
        Map<String, Object> input = new HashMap<>();
        input.put("backend", SECRETS_BACKEND.get());
        input.put("clearText", value);
        input.put("keyName", SECRETS_KEY_NAME.get());

        return Request.Post(SECRETS_URL.get() + CREATE_PATH)
            .bodyString(jsonMapper.writeValueAsString(input), ContentType.APPLICATION_JSON)
            .execute().handleResponse(new ResponseHandler<String>() {
                @Override
                public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode >= 300) {
                        throw new IOException("Failed to encrypt secret :" + response.getStatusLine().getReasonPhrase());
                    }
                    return IOUtils.toString(response.getEntity().getContent());
                }
            });
    }

    protected Map<Long, String> getValues(Collection<Secret> secrets, Host host) throws IOException {
        List<Secret> secretsList = new ArrayList<>(secrets);
        Map<Long, String> result = new HashMap<>();
        Map<String, Object> hostInfo = DataAccessor.fieldMap(host, HostConstants.FIELD_INFO);
        Object rewrapKey = CollectionUtils.getNestedValue(hostInfo, "hostKey", "data");

        Map<String, Object> input = new HashMap<>();
        input.put("data", toData(secretsList));
        input.put("rewrapKey", rewrapKey);
        Map<String, Object> response = Request.Post(SECRETS_URL.get() + BULK_PATH).
                bodyString(jsonMapper.writeValueAsString(input), ContentType.APPLICATION_JSON)
                .execute().handleResponse(new ResponseHandler<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode >= 300) {
                            throw new IOException("Failed to rewrap secret :" + response.getStatusLine().getReasonPhrase());
                        }
                        return jsonMapper.readValue(response.getEntity().getContent());
                    }
                });

        List<?> wrapped = CollectionUtils.toList(response.get("data"));
        for (int i = 0; i < secretsList.size(); i++) {
            Secret secret = secretsList.get(i);
            result.put(secret.getId(), CollectionUtils.toMap(wrapped.get(i)).get("rewrapText").toString());
        }

        return result;
    }

    protected List<Object> toData(List<Secret> secrets) throws IOException {
        List<Object> result = new ArrayList<>(secrets.size());
        for (Secret s : secrets) {
            result.add(jsonMapper.readValue(s.getValue()));
        }
        return result;
    }

    @Override
    public List<SecretValue> getValues(List<SecretReference> refs, Host host) throws IOException {
        Map<Long, Secret> secrets = secretDao.getSecrets(refs);
        Map<Long, String> values = getValues(secrets.values(), host);
        List<SecretValue> result = new ArrayList<>();

        for (SecretReference ref : refs) {
            Secret secret = secrets.get(ref.getSecretId());
            if (secret == null) {
                continue;
            }

            SecretValue value = new SecretValue(ref, secret, values.get(secret.getId()));
            result.add(value);
        }

        return result;
    }

}
