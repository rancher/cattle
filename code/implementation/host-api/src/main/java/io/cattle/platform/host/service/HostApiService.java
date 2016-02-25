package io.cattle.platform.host.service;

import io.cattle.platform.host.model.HostApiAccess;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.security.PublicKey;
import java.util.Date;
import java.util.Map;

public interface HostApiService {

    HostApiAccess getAccess(ApiRequest apiRequest, Long hostId, Map<String, Object> data, Date expiration, String... resourcePathSegments);

    HostApiAccess getAccess(ApiRequest apiRequest, Long hostId, Map<String, Object> data, String... resourcePathSegments);

    Map<String, PublicKey> getPublicKeys();

}