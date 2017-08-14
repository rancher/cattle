package io.cattle.platform.framework.secret;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.SecretReference;
import io.cattle.platform.core.model.Host;

import java.io.IOException;
import java.util.List;

public interface SecretsService {

    DynamicStringProperty SECRETS_KEY_NAME = ArchaiusUtil.getString("secrets.api.local.key.name");

    String encrypt(String value) throws IOException;

    String decrypt(String value) throws Exception;

    void delete(String value) throws IOException;

    List<SecretValue> getValues(List<SecretReference> refs, Host host) throws IOException;

}
