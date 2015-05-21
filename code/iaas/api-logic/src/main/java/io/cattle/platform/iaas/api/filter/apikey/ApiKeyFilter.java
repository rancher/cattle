package io.cattle.platform.iaas.api.filter.apikey;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import java.security.SecureRandom;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import com.netflix.config.DynamicStringProperty;

public class ApiKeyFilter extends AbstractDefaultResourceManagerFilter {

    public static final DynamicStringProperty BAD_CHARACTERS = ArchaiusUtil.getString("process.credential.create.bad.characters");
    private final static SecureRandom RANDOM = new SecureRandom();

    @Override
    public String[] getTypes() {
        return new String[] {CredentialConstants.KIND_API_KEY};
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Credential cred = request.proxyRequestObject(Credential.class);

        if (cred.getPublicValue() == null) {
            String[] keys = generateKeys();
            cred.setPublicValue(keys[0]);
            cred.setSecretValue(keys[1]);
        }

        return super.create(type, request, next);
    }

    public static String[] generateKeys() {
        byte[] accessKey = new byte[10];
        byte[] secretKey = new byte[128];

        RANDOM.nextBytes(accessKey);
        RANDOM.nextBytes(secretKey);

        String accessKeyString = Hex.encodeHexString(accessKey);
        String secretKeyString = Base64.encodeBase64String(secretKey).replaceAll(BAD_CHARACTERS.get(), "");

        if (secretKeyString.length() < 40) {
            /* Wow, this is terribly bad luck */
            throw new IllegalStateException("Failed to create secretKey due to not enough good characters");
        }

        return new String[] { accessKeyString.substring(0, 20).toUpperCase(), secretKeyString.substring(0, 40) };
    }

}