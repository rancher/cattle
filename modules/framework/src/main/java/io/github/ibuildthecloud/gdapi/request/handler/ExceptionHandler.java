package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.model.ApiError;
import io.github.ibuildthecloud.gdapi.model.impl.ErrorImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ExceptionUtils;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionHandler implements ApiRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);

    Properties standardErrorCodes;
    String messageLocation;
    String messageLocationOverride;
    boolean throwUnknownErrors = false;

    @Override
    public void handle(ApiRequest request) throws IOException {
    }

    @Override
    public boolean handleException(ApiRequest apiRequest, Throwable t) throws IOException, ServletException {
        ApiError error = getError(apiRequest, t);
        if (error != null) {
            apiRequest.setResponseCode(error.getStatus());
            apiRequest.setResponseObject(error);
            return true;
        }
        return false;
    }

    protected ApiError getError(ApiRequest apiRequest, Throwable t) throws IOException, ServletException {
        if (t instanceof ClientVisibleException) {
            return getError(apiRequest, (ClientVisibleException)t);
        } else {
            return getUnknownError(apiRequest, t);
        }
    }

    protected ApiError getError(ApiRequest apiRequest, ClientVisibleException e) {
        if (e.getApiError() != null) {
            return e.getApiError();
        }

        ErrorImpl errorImpl = new ErrorImpl(e);
        return populateError(errorImpl, apiRequest.getLocale());
    }

    protected ApiError getUnknownError(ApiRequest apiRequest, Throwable t) throws IOException, ServletException {
        if (throwUnknownErrors) {
            log.error("Rethrowing exception in API for request [{}]", apiRequest, t);
            ExceptionUtils.rethrowRuntime(t);
            ExceptionUtils.rethrow(t, IOException.class);
            ExceptionUtils.rethrow(t, ServletException.class);
            throw new ServletException(t);
        } else {
            ErrorImpl e = new ErrorImpl(ResponseCodes.INTERNAL_SERVER_ERROR);
            log.error("Exception in API for request [{}]. Error id: [{}].", apiRequest, e.getId(), t);
            return populateError(e, apiRequest.getLocale());
        }
    }

    protected ApiError populateError(ErrorImpl error, Locale locale) {
        if (error.getCode() == null) {
            if (standardErrorCodes != null) {
                error.setCode(standardErrorCodes.getProperty(Integer.toString(error.getStatus())));
            }
            if (error.getCode() == null) {
                error.setCode(Integer.toString(error.getStatus()));
            }
        }

        if (error.getMessage() == null) {
            error.setMessage(error.getCode());
        }

        error.setMessage(getMessage(error.getMessage(), locale));
        error.setDetail(getMessage(error.getDetail(), locale));

        return error;
    }

    protected String getMessage(String messageCode, Locale locale) {
        if (messageCode == null) {
            return messageCode;
        }

        String message = null;
        message = getLocalizedMessage(messageLocationOverride, messageCode, locale);

        if (message == null) {
            message = getLocalizedMessage(messageLocation, messageCode, locale);
        }

        return message == null ? messageCode : message;
    }

    protected String getLocalizedMessage(String location, String messageCode, Locale locale) {
        if (locale == null || location == null)
            return null;

        ResourceBundle bundle = ResourceBundle.getBundle(messageLocation, locale);
        if (bundle != null) {
            try {
                return bundle.getString(messageCode);
            } catch (MissingResourceException e) {
                // ignore
            }
        }

        return null;
    }

    public Properties getStandardErrorCodes() {
        return standardErrorCodes;
    }

    public void setStandardErrorCodes(Properties standardErrorCodes) {
        this.standardErrorCodes = standardErrorCodes;
    }

    public String getMessageLocation() {
        return messageLocation;
    }

    public void setMessageLocation(String messageLocation) {
        this.messageLocation = messageLocation;
    }

    public String getMessageLocationOverride() {
        return messageLocationOverride;
    }

    public void setMessageLocationOverride(String messageLocationOverride) {
        this.messageLocationOverride = messageLocationOverride;
    }

    public boolean isThrowUnknownErrors() {
        return throwUnknownErrors;
    }

    public void setThrowUnknownErrors(boolean throwUnknownErrors) {
        this.throwUnknownErrors = throwUnknownErrors;
    }

}
