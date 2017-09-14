package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Credential;

public interface RegistrationTokenAuthDao {

    Credential getCredential(String accessKey);

}
