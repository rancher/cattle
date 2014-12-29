package io.cattle.platform.iaas.api.auth.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.config.DynamicStringProperty;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.token.impl.JwtTokenServiceImpl;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public class GithubOAuthImpl implements AccountLookup, Priority {

	private static final String AUTH_HEADER = "Authorization";
	private static final String GITHUB_REQUEST_TOKEN = "code";
	private static final String CLIENT_ID = "client_id";
	private static final String CLIENT_SECRET = "client_secret";

	private TokenService tokenService;

	private static Client client;

	static {
		ClientConfig config = new DefaultClientConfig();
		config.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, 1000);
		config.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, 1000);
		client = Client.create(config);
	}

	private static final DynamicStringProperty GITHUB_CLIENT_ID = ArchaiusUtil
			.getString("api.auth.github.client.id");
	private static final DynamicStringProperty GITHUB_CLIENT_SECRET = ArchaiusUtil
			.getString("api.auth.github.client.secret");
	public static final DynamicStringProperty GITHUB_URL = ArchaiusUtil
			.getString("api.auth.github.url");
	public static final ObjectMapper mapper = new ObjectMapper();

	@Override
	public int getPriority() {
		return Priority.DEFAULT;
	}

	@Override
	public Account getAccount(ApiRequest request) {
		String token = request.getServletContext().getRequest()
				.getHeader(AUTH_HEADER);
		if (StringUtils.isEmpty(token)) {
			token = getNewGithubAccessToken(request);
		}
		if (StringUtils.isNotEmpty(token) && isvalidGithubAccessToken(token)) {
			Account account = getOrCreateAccounts(token);
			return account;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private String getNewGithubAccessToken(ApiRequest request) {
		String code = request.getServletContext().getRequest()
				.getHeader(GITHUB_REQUEST_TOKEN);
		if (StringUtils.isEmpty(code)) {
			return null;
		}
		Map<String, String> requestObj = new HashMap<String, String>();
		requestObj.put(CLIENT_ID, GITHUB_CLIENT_ID.get());
		requestObj.put(CLIENT_SECRET, GITHUB_CLIENT_SECRET.get());
		requestObj.put(GITHUB_REQUEST_TOKEN, code);
		WebResource resource = client.resource(GITHUB_URL.get());
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, requestObj);
		if (response.getStatus() > 201) {
			return null;
		}
		Map<String, Object> responseMap = null;
		try {
			responseMap = mapper.readValue(response.getEntityInputStream(),
					Map.class);
		} catch (IOException e) {
			return null;
		}
		return tokenService.generateEncryptedToken(responseMap);
	}

	private boolean isvalidGithubAccessToken(String token) {
		try {
			return tokenService.isValidToken(token, true);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean challenge(ApiRequest request) {
		return false;
	}

	@Inject
	public void setJwtTokenService(TokenService tokenService) {
		this.tokenService = tokenService;
	}
}
