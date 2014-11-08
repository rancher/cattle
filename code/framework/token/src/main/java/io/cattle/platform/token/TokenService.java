package io.cattle.platform.token;

import java.util.Map;

public interface TokenService {

    String generateToken(Map<String,Object> payload);

}
