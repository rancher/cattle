package io.cattle.platform.host.service;

import io.cattle.platform.host.model.HostApiAccess;

import java.security.PublicKey;
import java.util.Map;

public interface HostApiService {

    HostApiAccess getAccess(Long hostId);

    Map<String,PublicKey> getPublicKeys();

}