package io.cattle.platform.iaas.api.auth.integration.external;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AuthToken;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.token.TokenException;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ExternalServiceAuthProvider {

    private static final Logger log = LoggerFactory.getLogger(ExternalServiceAuthProvider.class);

    @Inject
    private JsonMapper jsonMapper;

    @Inject
    TokenService tokenService;

    @Inject
    ExternalServiceTokenUtil tokenUtil;
    @Inject
    private AuthTokenDao authTokenDao;

    public Token getToken(ApiRequest request) {
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        String code = ObjectUtils.toString(requestBody.get(SecurityConstants.CODE));

        //get the token from the auth service
        StringBuilder authUrl = new StringBuilder(ServiceAuthConstants.AUTH_SERVICE_URL.get());
        authUrl.append("/token");

        HttpResponse response;
        try {
            Map<String, String> data = new HashMap<String, String>();
            data.put("code", code);
            String jsonString = jsonMapper.writeValueAsString(data);

            Request temp = Request.Post(authUrl.toString())
                    .addHeader(ServiceAuthConstants.ACCEPT, ServiceAuthConstants.APPLICATION_JSON)
                    .bodyString(jsonString, ContentType.APPLICATION_JSON);
            response = temp.execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode >= 300) {
                log.error("Got error from Auth service. statusCode: {}", statusCode);
                throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, ServiceAuthConstants.AUTH_ERROR,
                        "Error Response from Auth service", "Status code from Auth Service: " + Integer.toString(statusCode));
            }
            Map<String, Object> jsonData = jsonMapper.readValue(response.getEntity().getContent());

            String encryptedToken = (String)jsonData.get(ServiceAuthConstants.JWT_KEY);
            Map<String, Object> decryptedToken = tokenService.getJsonPayload(encryptedToken, false);
            String accessToken = (String)decryptedToken.get("access_token");
            request.setAttribute(ServiceAuthConstants.ACCESS_TOKEN, accessToken);
            List<?> identityList = CollectionUtils.toList(jsonData.get("identities"));
            Set<Identity> identities = new HashSet<>();
            if (identityList != null && !identityList.isEmpty())
            {
                for(Object identity : identityList) {
                    Map<String, Object> jsonIdentity = CollectionUtils.toMap(identity);
                    identities.add(tokenUtil.jsonToIdentity(jsonIdentity));
                }
            }

            Token token = tokenUtil.createToken(identities, null);

            return token;
        } catch(HttpHostConnectException ex) {
            log.error("Auth Service not reachable at [{}]", ServiceAuthConstants.AUTH_SERVICE_URL);
            return null;
        } catch (IOException e) {
            log.error("Failed to get token from Auth Service.", e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, ServiceAuthConstants.AUTH_ERROR,
                    "Failed to get Auth token.", null);
        } catch (TokenException e) {
            log.error("Failed to decrypt the token from Auth Service.", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Token refreshToken(String accessToken) {
        //get the token from the auth service
        StringBuilder authUrl = new StringBuilder(ServiceAuthConstants.AUTH_SERVICE_URL.get());
        authUrl.append("/token");

        HttpResponse response;
        try {
            Map<String, String> data = new HashMap<String, String>();
            data.put("accessToken", accessToken);
            String jsonString = jsonMapper.writeValueAsString(data);

            Request temp = Request.Post(authUrl.toString()).addHeader(ServiceAuthConstants.ACCEPT, ServiceAuthConstants.APPLICATION_JSON)
                    .bodyString(jsonString, ContentType.APPLICATION_JSON);
            response = temp.execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode >= 300) {
                log.error("Got error from Auth service. statusCode: {}", statusCode);
                throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, ServiceAuthConstants.AUTH_ERROR,
                        "Error Response from Auth service", "Status code from Auth Service: " + Integer.toString(statusCode));
            }
            Map<String, Object> jsonData = jsonMapper.readValue(response.getEntity().getContent());

            String encryptedToken = (String)jsonData.get(ServiceAuthConstants.JWT_KEY);
            Map<String, Object> decryptedToken = tokenService.getJsonPayload(encryptedToken, true);
            Token token = tokenUtil.createToken((Set<Identity>)decryptedToken.get("identities"), null);

            return token;
        } catch(HttpHostConnectException ex) {
            log.error("Auth Service not reachable at [{}]", ServiceAuthConstants.AUTH_SERVICE_URL);
            return null;
        } catch (IOException e) {
            log.error("Failed to get token from Auth Service.", e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, ServiceAuthConstants.AUTH_ERROR,
                    "Failed to get Auth token.", null);
        } catch (TokenException e) {
            log.error("Failed to decrypt the token from Auth Service.", e);
            return null;
        }
    }

    public List<Identity> searchIdentities(String name, boolean exactMatch) {
        if (!isConfigured()) {
            return new ArrayList<Identity>();
        }
        List<Identity> identities = new ArrayList<>();
        StringBuilder authUrl = new StringBuilder(ServiceAuthConstants.AUTH_SERVICE_URL.get());
        HttpResponse response;
        try {
            authUrl.append("/identities?name=").append(URLEncoder.encode(name, "UTF-8"));
            Request temp = Request.Get(authUrl.toString()).addHeader(ServiceAuthConstants.ACCEPT, ServiceAuthConstants.APPLICATION_JSON);
            String externalAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(ServiceAuthConstants.ACCESS_TOKEN);
            String bearerToken = " Bearer "+ externalAccessToken;
            temp.addHeader(ServiceAuthConstants.AUTHORIZATION, bearerToken);
            response = temp.execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode >= 300) {
                log.error("Got error from Auth service. statusCode: {}", statusCode);
                throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, ServiceAuthConstants.AUTH_ERROR,
                        "Error Response from Auth service", "Status code from Auth Service: " + Integer.toString(statusCode));
            }
            Map<String, Object> jsonData = jsonMapper.readValue(response.getEntity().getContent());
            List<?> identityList = CollectionUtils.toList(jsonData.get("data"));
            if (identityList != null && !identityList.isEmpty())
            {
                for(Object identity : identityList) {
                    Map<String, Object> jsonIdentity = CollectionUtils.toMap(identity);
                    identities.add(tokenUtil.jsonToIdentity(jsonIdentity));
                }
            }

        } catch(HttpHostConnectException ex) {
            log.error("Auth Service not reachable at [{}]", ServiceAuthConstants.AUTH_SERVICE_URL);
        } catch (ClientVisibleException e) {
            log.error("Failed to search identities from Auth Service.", e);
        } catch (Exception e) {
            log.error("Failed to search identities from Auth Service.", e);
        }
        return identities;
    }

    public Identity getIdentity(String id, String scope) {
        if (!isConfigured()) {
            return null;
        }
        //check if the setting 'support.identity.lookup = false', if yes then lookup the identity from token

        if(ServiceAuthConstants.NO_IDENTITY_LOOKUP_SUPPORTED.get()) {
            log.debug("Identity lookup is not supported at the provider");
            if (tokenUtil.findAndSetJWT()) {
                Set<Identity> identitiesInToken = tokenUtil.getIdentities();
                log.debug("Found identitiesInToken {}" , identitiesInToken);
                for (Identity identity : identitiesInToken) {
                    if(identity != null && id.equals(identity.getExternalId()) && scope.equals(identity.getExternalIdType())) {
                        return identity;
                    }
                }
            }
        }

        StringBuilder authUrl = new StringBuilder(ServiceAuthConstants.AUTH_SERVICE_URL.get());

        HttpResponse response;
        try {
            authUrl.append("/identities?externalId=").append(URLEncoder.encode(id, "UTF-8")).append("&externalIdType=")
            .append(URLEncoder.encode(scope, "UTF-8"));
            Request temp = Request.Get(authUrl.toString()).addHeader(ServiceAuthConstants.ACCEPT, ServiceAuthConstants.APPLICATION_JSON);
            String externalAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(ServiceAuthConstants.ACCESS_TOKEN);
            String bearerToken = " Bearer "+ externalAccessToken;
            temp.addHeader(ServiceAuthConstants.AUTHORIZATION, bearerToken);
            response = temp.execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode >= 300) {
                log.error("Got error from Auth service. statusCode: {}", statusCode);
                throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, ServiceAuthConstants.AUTH_ERROR,
                        "Error Response from Auth service", "Status code from Auth Service: " + Integer.toString(statusCode));
            }
            Map<String, Object> jsonData = jsonMapper.readValue(response.getEntity().getContent());
            return tokenUtil.jsonToIdentity(jsonData);

        } catch(HttpHostConnectException ex) {
            log.error("Auth Service not reachable at [{}]", ServiceAuthConstants.AUTH_SERVICE_URL);
            return null;
        } catch (IOException e) {
            log.error("Failed to get token from Auth Service.", e);
            return null;
        }
    }

    public Set<Identity> getIdentities(Account account) {
        if (!isConfigured()) {
            return new HashSet<>();
        }
        String accessToken = (String) DataAccessor.fields(account).withKey(ServiceAuthConstants.ACCESS_TOKEN).get();
        if (tokenUtil.findAndSetJWT()) {
            ApiRequest request = ApiContext.getContext().getApiRequest();
            request.setAttribute(ServiceAuthConstants.ACCESS_TOKEN, accessToken);
            return tokenUtil.getIdentities();
        }
        String jwt = null;
        if (!StringUtils.isBlank(accessToken) && SecurityConstants.SECURITY.get()) {
            AuthToken authToken = authTokenDao.getTokenByAccountId(account.getId());
            if (authToken == null) {
                try {
                    //refresh token API.
                    Token token = refreshToken(accessToken);
                    if (token != null) {
                        jwt = ProjectConstants.AUTH_TYPE + token.getJwt();
                        authToken = authTokenDao.createToken(token.getJwt(), token.getAuthProvider(), account.getId());
                        jwt = authToken.getKey();
                    }
                } catch (ClientVisibleException e) {
                        log.error("Got error from Auth service.error", e);
                        return Collections.emptySet();
                }
            } else {
                jwt = authToken.getKey();
            }

        }
        if (StringUtils.isBlank(jwt)){
            return Collections.emptySet();
        }
        ApiRequest request = ApiContext.getContext().getApiRequest();
        request.setAttribute(tokenUtil.tokenType(), jwt);
        request.setAttribute(ServiceAuthConstants.ACCESS_TOKEN, accessToken);
        return tokenUtil.getIdentities();
    }

    public boolean isConfigured() {
        if (SecurityConstants.AUTH_PROVIDER.get() != null
                && !SecurityConstants.NO_PROVIDER.equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get())
                && !SecurityConstants.INTERNAL_AUTH_PROVIDERS.contains(SecurityConstants.AUTH_PROVIDER.get())
                && ServiceAuthConstants.IS_EXTERNAL_AUTH_PROVIDER.get()) {
            return true;
        }
        return false;
    }

    public Identity untransform(Identity identity) {
        return identity;
    }

    public Identity transform(Identity identity) {
        return identity;
    }

    public String getRedirectUrl() {
        StringBuilder authUrl = new StringBuilder(ServiceAuthConstants.AUTH_SERVICE_URL.get());
        authUrl.append("/redirectUrl");

        HttpResponse response;
        try {
            Request temp = Request.Get(authUrl.toString()).addHeader(ServiceAuthConstants.ACCEPT, ServiceAuthConstants.APPLICATION_JSON);
            response = temp.execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode >= 300) {
                log.error("Got error from Auth service. statusCode: {}", statusCode);
                return "";
            }
            Map<String, Object> jsonData = jsonMapper.readValue(response.getEntity().getContent());
            if( jsonData != null && !jsonData.isEmpty()) {
                if (jsonData.containsKey("redirectUrl")) {
                    return (String)jsonData.get("redirectUrl");
                }
            };
        } catch(HttpHostConnectException ex) {
            log.error("Auth Service not reachable at [{}]", ServiceAuthConstants.AUTH_SERVICE_URL);
        } catch (IOException e) {
            log.error("Failed to get the redirectUrl from Auth Service.", e);
        }
        return "";
    }

    public Token readCurrentToken() {
        Token token = new Token();

        //get redirect Url from external service
        String redirect = getRedirectUrl();
        token.setRedirectUrl(redirect);

        if (tokenUtil.findAndSetJWT()) {
            Token userToken = tokenUtil.getUserIdentityFromJWT();
            if(userToken != null) {
                token.setUserIdentity(userToken.getUserIdentity());
                token.setUserType(userToken.getUserType());
            }
        }
        log.info("readCurrentToken returning {}", token);
        return token;
    }

}
