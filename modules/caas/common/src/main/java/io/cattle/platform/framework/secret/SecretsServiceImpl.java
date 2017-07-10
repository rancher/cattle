package io.cattle.platform.framework.secret;

import com.netflix.config.DynamicStringProperty;
import com.nimbusds.jose.util.StandardCharset;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.SecretReference;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.dao.SecretDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Secret;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.ssh.common.SshKeyGen;
import io.cattle.platform.token.impl.RSAKeyProvider;
import io.cattle.platform.token.impl.RSAPrivateKeyHolder;
import io.cattle.platform.util.type.CollectionUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecretsServiceImpl implements SecretsService {

    private static final String CREATE_PATH = "/v1-secrets/secrets/create";
    private static final String PURGE_PATH = "/v1-secrets/secrets/purge";
    private static final String REWRAP = "/v1-secrets/secrets/rewrap";
    private static final String BULK_PATH = "/v1-secrets/secrets/rewrap?action=bulk";
    private static final DynamicStringProperty SECRETS_URL = ArchaiusUtil.getString("secrets.url");
    private static final DynamicStringProperty SECRETS_BACKEND = ArchaiusUtil.getString("secrets.backend");

    SecretDao secretDao;
    JsonMapper jsonMapper;
    RSAKeyProvider rsaKeyProvider;

    public SecretsServiceImpl(SecretDao secretDao, JsonMapper jsonMapper, RSAKeyProvider rsaKeyProvider) {
        super();
        this.secretDao = secretDao;
        this.jsonMapper = jsonMapper;
        this.rsaKeyProvider = rsaKeyProvider;
    }

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
                public String handleResponse(HttpResponse response) throws IOException {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode >= 300) {
                        throw new IOException("Failed to encrypt secret :" + response.getStatusLine().getReasonPhrase());
                    }
                    return IOUtils.toString(response.getEntity().getContent(), StandardCharset.UTF_8);
                }
            });
    }

    @Override
    public void delete(long accountId, String value) throws IOException {
        if (StringUtils.isBlank(value)) {
            return;
        }
        Request.Post(SECRETS_URL.get() + PURGE_PATH).bodyString(value, ContentType.APPLICATION_JSON).execute().handleResponse((response) -> {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 300 && statusCode != 404) {
                throw new IOException("Failed to delete secret :" + response.getStatusLine().getReasonPhrase());
            }
            return IOUtils.toString(response.getEntity().getContent(), StandardCharset.UTF_8);
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
                    public Map<String, Object> handleResponse(HttpResponse response) throws IOException {
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

    @Override
    public String decrypt(long accountId, String value) throws Exception {
        RSAPrivateKeyHolder holder = rsaKeyProvider.getPrivateKey();
        PublicKey publicKey = rsaKeyProvider.getPublicKeys().get(holder.getKeyId());
        String encoded = SshKeyGen.toPEM(publicKey);

        Map<String, Object> input = jsonMapper.readValue(value);
        input.put("rewrapKey", encoded);

        String encrypted = Request.Post(SECRETS_URL.get() + REWRAP)
            .bodyString(jsonMapper.writeValueAsString(input), ContentType.APPLICATION_JSON)
            .execute().handleResponse(new ResponseHandler<String>() {
                @Override
                public String handleResponse(HttpResponse response) throws IOException {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode >= 300) {
                        throw new IOException("Failed to rewrap secret :" + response.getStatusLine().getReasonPhrase());
                    }
                    return IOUtils.toString(response.getEntity().getContent(), StandardCharset.UTF_8);
                }
            });

        return unwrap(holder.getKey(), encrypted);
    }

    protected String unwrap(PrivateKey privateKey, String encrypted) throws Exception {
        Map<String, Object> rewrappedSecrect = jsonMapper.readValue(encrypted);
        Map<String, Object> encryptedObject = jsonMapper.readValue(Base64.decodeBase64((String) rewrappedSecrect.get("rewrapText")));
        Map<String, Object> encryptedText = jsonMapper.readValue((String)encryptedObject.get("encryptedText"));
        @SuppressWarnings("unchecked")
        Map<String, Object> encryptedKey = (Map<String, Object>)encryptedObject.get("encryptedKey");

        byte[] encryptionKey = getEncryptionKey(privateKey, encryptedKey);
        byte[] nonce = Base64.decodeBase64((String)encryptedText.get("Nonce"));
        byte[] cipherText = Base64.decodeBase64((String)encryptedText.get("CipherText"));

        byte[] decrypted = decrypt(cipherText, encryptionKey, nonce);

        return new String(decrypted);
    }

    protected byte[] getEncryptionKey(PrivateKey key, Map<String, Object> encryptedKey) throws Exception {
        Cipher c = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding", "BC");
        c.init(Cipher.DECRYPT_MODE, key);
        return c.doFinal(Base64.decodeBase64((String) encryptedKey.get("encryptedText")));
    }

    protected byte[] decrypt(byte[] cipherText, byte[] key, byte[] nonce) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(nonce));
        return c.doFinal(cipherText);
    }

}
