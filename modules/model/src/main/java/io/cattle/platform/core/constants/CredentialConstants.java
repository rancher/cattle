package io.cattle.platform.core.constants;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CredentialConstants {

    private final static SecureRandom RANDOM = new SecureRandom();

    public static final String TYPE = "credential";

    public static final DynamicStringProperty BAD_CHARACTERS = ArchaiusUtil.getString("process.credential.create.bad.characters");

    public static final String KIND_API_KEY = "apiKey";
    public static final String KIND_PASSWORD = "password";
    public static final String KIND_AGENT_API_KEY = "agentApiKey";
    public static final String KIND_CREDENTIAL_REGISTRATION_TOKEN = "registrationToken";

    public static final String FIELD_CLUSTER_ID = "clusterId";

    public static final String LINK_CERTIFICATE = "certificate";

    public static final String KIND_REGISTRY_CREDENTIAL = "registryCredential";
    public static final String PUBLIC_VALUE = "publicValue";

    public static final String PROCESSS_DEACTIVATE = "credential.deactivate";
    public static final String PROCESSS_REMOVE = "credential.remove";

    public static final Set<String> CREDENTIAL_TYPES_TO_FILTER = Collections.unmodifiableSet(new HashSet<>(Arrays
            .asList(
                    KIND_API_KEY, KIND_PASSWORD
            )));

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
