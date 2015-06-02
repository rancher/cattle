package io.cattle.platform.storage;

import io.cattle.platform.core.model.Credential;

import java.util.List;

public interface ImageCredentialLookup {

    Credential getDefaultCredential(String uuid, long currentAccount);
}
