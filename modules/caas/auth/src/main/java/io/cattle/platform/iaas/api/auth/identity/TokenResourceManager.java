package io.cattle.platform.iaas.api.auth.identity;

import com.netflix.config.DynamicBooleanProperty;
import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.pubsub.manager.SubscribeManager;
import io.cattle.platform.api.resource.AbstractNoOpResourceManager;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.iaas.api.auth.integration.external.ExternalServiceAuthProvider;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenCreator;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenResourceManager extends AbstractNoOpResourceManager {

    private static final DynamicBooleanProperty RESTRICT_CONCURRENT_SESSIONS = ArchaiusUtil.getBoolean("api.auth.restrict.concurrent.sessions");
    private static final Map<String, Object> LOGOUT_MESSAGE = new HashMap<>();

    static {
        LOGOUT_MESSAGE.put("name", "logout");
    }


    AuthTokenDao authTokenDao;
    IdentityManager identityManager;
    ExternalServiceAuthProvider externalAuthProvider;
    EventService eventService;
    List<TokenCreator> tokenCreators;

    public TokenResourceManager(AuthTokenDao authTokenDao, IdentityManager identityManager, ExternalServiceAuthProvider externalAuthProvider,
            EventService eventService, List<TokenCreator> tokenCreators) {
        super();
        this.authTokenDao = authTokenDao;
        this.identityManager = identityManager;
        this.externalAuthProvider = externalAuthProvider;
        this.eventService = eventService;
        this.tokenCreators = tokenCreators;
    }

    @Override
    public Object create(String type, ApiRequest request) {
        if (!StringUtils.equals(AbstractTokenUtil.TOKEN, request.getType())) {
            return null;
        }
        return createToken(request);
    }

    private Token createToken(ApiRequest request) {
        Token token = null;

        if (SecurityConstants.AUTH_PROVIDER.get() == null || SecurityConstants.NO_PROVIDER.equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get())) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                    "NoAuthProvider", "No Auth provider is configured.", null);
        }

        if (SecurityConstants.INTERNAL_AUTH_PROVIDERS.contains(SecurityConstants.AUTH_PROVIDER.get())) {
            for (TokenCreator tokenCreator : tokenCreators) {
                if (tokenCreator.isConfigured() && tokenCreator.providerType().equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get())) {
                    if (!SecurityConstants.SECURITY.get()) {
                        tokenCreator.reset();
                    }
                    token = tokenCreator.getToken(request);
                    break;
                }
            }
        } else {
            //call external service
            token = externalAuthProvider.getToken(request);
        }

        if (token == null){
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                    "codeInvalid", "Code provided is invalid.", null);
        }
        Identity[] identities = token.getIdentities();
        List<Identity> transFormedIdentities = new ArrayList<>();
        for (Identity identity : identities) {
            transFormedIdentities.add(identityManager.untransform(identity, true));
        }
        token.setIdentities(transFormedIdentities);
        token.setUserIdentity(identityManager.untransform(token.getUserIdentity(), true));

        long authenticatedAsAccountId = token.getAuthenticatedAsAccountId();
        long tokenAccountId = ((Policy) ApiContext.getContext().getPolicy()).getAccountId();

        if (RESTRICT_CONCURRENT_SESSIONS.get()) {
            authTokenDao.deletePreviousTokens(authenticatedAsAccountId, tokenAccountId);
            String event = FrameworkEvents.appendAccount(SubscribeManager.EVENT_DISCONNECT, authenticatedAsAccountId);
            eventService.publish(EventVO.newEvent(event).withData(LOGOUT_MESSAGE));
        }

        token.setJwt(authTokenDao.createToken(token.getJwt(), token.getAuthProvider(),
                ((Policy) ApiContext.getContext().getPolicy()).getAccountId(), authenticatedAsAccountId).getKey());

        return token;
    }

    @Override
    public Object listSupport(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        Token token = listToken();
        return Collections.singletonList(token);
    }

    protected Token listToken() {
        Token token = new Token();

        if (SecurityConstants.AUTH_PROVIDER.get() == null || SecurityConstants.NO_PROVIDER.equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get())) {
            return token;
        }

        if (SecurityConstants.INTERNAL_AUTH_PROVIDERS.contains(SecurityConstants.AUTH_PROVIDER.get())) {
            for (TokenCreator tokenCreator : tokenCreators) {
                if (tokenCreator.isConfigured() && tokenCreator.providerType().equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get())) {
                    token = tokenCreator.getCurrentToken();
                    break;
                }
            }
            return token;
        } else {
            //get redirect Url from external service
            if (externalAuthProvider.isConfigured()) {
                return externalAuthProvider.readCurrentToken();
            }
        }
        return token;
    }

    @Override
    public Object deleteObjectSupport(String type, String id, Object obj, ApiRequest request) {
        if (!StringUtils.equals(AbstractTokenUtil.TOKEN, request.getType())) {
            return null;
        }
        return deleteToken(obj, request);
    }

    protected Object deleteToken(Object obj, ApiRequest request) {
        Token token = new Token();
        String jwt = "";

        token = listToken();
        jwt = token.getJwt();

        if(StringUtils.isBlank(jwt)) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                    "JWTNotProvided", "Request does not contain JWT cookie", null);
        }

        request.setResponseCode(ResponseCodes.NO_CONTENT);
        HttpServletResponse response = request.getServletContext().getResponse();
        String cookieString="token=;Path=/;Expires=Thu, 01 Jan 1970 00:00:00 GMT;";
        response.addHeader("Set-Cookie", cookieString);
        request.getServletContext().setResponse(response);
        if(authTokenDao.deleteToken(jwt)) {
            return obj;
        }
        return null;
    }
}
