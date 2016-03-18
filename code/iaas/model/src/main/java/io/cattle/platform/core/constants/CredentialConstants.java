package io.cattle.platform.core.constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CredentialConstants {

    public static final String TYPE = "credential";

    public static final String KIND_API_KEY = "apiKey";
    public static final String KIND_PASSWORD = "password";
    public static final String KIND_AGENT_API_KEY = "agentApiKey";
    public static final String KIND_SSH_KEY = "sshKey";

    public static final String LINK_PEM_FILE = "pem";
    public static final String LINK_CERTIFICATE = "certificate";

    public static final String KIND_REGISTRY_CREDENTIAL = "registryCredential";
    public static final String PUBLIC_VALUE = "publicValue";

    public static final String PROCESSS_DEACTIVATE = "credential.deactivate";
    public static final String PROCESSS_REMOVE = "credential.remove";

    public static final Set<String> CREDENTIAL_TYPES_TO_FILTER = Collections.unmodifiableSet(new HashSet<>(Arrays
            .asList(
                    KIND_API_KEY, KIND_PASSWORD
            )));
}
