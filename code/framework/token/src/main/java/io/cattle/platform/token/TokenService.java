package io.cattle.platform.token;

import java.util.Map;

import com.nimbusds.jose.JOSEException;

import net.minidev.json.JSONObject;

public interface TokenService {

	String generateToken(Map<String, Object> payload);
	
	String generateEncryptedToken(Map<String, Object> payload);

	boolean isValidToken(String token, boolean encrypted) throws JOSEException;

	JSONObject getJsonPayload(String token, boolean encrypted) throws JOSEException;

}
