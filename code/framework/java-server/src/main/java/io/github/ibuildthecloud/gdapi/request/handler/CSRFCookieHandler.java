package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.io.IOException;
import java.security.SecureRandom;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSRFCookieHandler extends AbstractApiRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(CSRFCookieHandler.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    public static final String CSRF = "CSRF";
    public static final String HEADER = "X-API-CSRF";

    @Override
    public void handle(ApiRequest request) throws IOException {
        HttpServletRequest httpRequest = request.getServletContext().getRequest();
        HttpServletResponse response = request.getServletContext().getResponse();

        if (!RequestUtils.isBrowser(httpRequest, false)) {
            return;
        }

        Cookie csrf = null;
        Cookie[] cookies = httpRequest.getCookies();

        if (cookies != null) {
            for (Cookie c : httpRequest.getCookies()) {
                if (CSRF.equals(c.getName()) && c.getName() != null) {
                    csrf = c;
                    break;
                }
            }
        }

        if (csrf == null) {
            byte[] bytes = new byte[5];
            RANDOM.nextBytes(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X", b));
            }
            csrf = new Cookie(CSRF, sb.toString());
        } else if (!Method.GET.isMethod(request.getMethod())) {
            /*
             * Very important to use request.getMethod() and not httpRequest.getMethod(). The client can override the HTTP method with _method
             */
            if (csrf.getValue().equals(httpRequest.getHeader(HEADER))) {
                // Good
            } else if (csrf.getValue().equals(httpRequest.getParameter(CSRF))) {
                // Good
            } else {
                log.warn("Request's CSRF header did not match cookie");
                throw new ClientVisibleException(ResponseCodes.FORBIDDEN, ValidationErrorCodes.INVALID_CSRF_TOKEN);
            }
        }

        csrf.setPath("/");
        response.addCookie(csrf);
    }

}