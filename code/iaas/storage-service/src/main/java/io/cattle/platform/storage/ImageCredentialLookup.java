package io.cattle.platform.storage;

import io.cattle.platform.core.model.Credential;

public interface ImageCredentialLookup {

    Credential getDefaultCredential(String uuid, long currentAccount);
}
