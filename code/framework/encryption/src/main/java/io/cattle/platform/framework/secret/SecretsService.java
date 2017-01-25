package io.cattle.platform.framework.secret;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.SecretReference;
import io.cattle.platform.core.model.Host;

import java.io.IOException;
import java.util.List;

import com.netflix.config.DynamicStringProperty;

public interface SecretsService {

    final DynamicStringProperty SECRETS_KEY_NAME = ArchaiusUtil.getString("secrets.api.local.key.name");

    String encrypt(long accountId, String value) throws IOException;

    List<SecretValue> getValues(List<SecretReference> refs, Host host) throws IOException;

}
