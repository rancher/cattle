package io.cattle.platform.app;

import io.cattle.platform.api.servlet.IndexFile;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.util.exception.ExceptionUtils;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringProperty;

public class CattleFilter implements Filter {

    private static final DynamicStringListProperty IGNORE = ArchaiusUtil.getList("api.ignore.paths");
    private static final DynamicStringProperty PL_SETTING = ArchaiusUtil.getString("ui.pl");
    private static final String PL = "PL";
    private static final String LANG = "LANG";
    private static final String VERSION = "X-Rancher-Version";
    private static final String XFRAME = "X-Frame-Options";
    private static final String DENY = "DENY";
    private static final DynamicStringProperty LOCALIZATION = ArchaiusUtil.getString("localization");
    private static final DynamicStringProperty SERVER_VERSION = ArchaiusUtil.getString("rancher.server.version");

    Cattle cattle;
    IndexFile indexFile = new IndexFile();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        indexFile.init();
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        String path = httpRequest.getServletPath();

        boolean ignore = false;
        for (String prefix : IGNORE.get()) {
            if (path.startsWith(prefix)) {
                ignore = true;
                break;
            }
        }

        if (ignore) {
            chain.doFilter(request, response);
            return;
        }

        addPLCookie(httpRequest, (HttpServletResponse) response);
        addDefaultLanguageCookie(httpRequest, (HttpServletResponse) response);
        addVersionHeader(httpRequest, (HttpServletResponse) response);

        if (isUIRequest(httpRequest, path)) {
            addUIHeaders(httpRequest, (HttpServletResponse) response);
            if (path.contains(".") || !indexFile.canServeContent()) {
                chain.doFilter(request, response);
                return;
            } else {
                indexFile.serveIndex((HttpServletRequest)request, (HttpServletResponse) response);
                return;
            }
        }

        try {
            new ManagedContextRunnable() {
                @Override
                protected void runInContext() {
                    try {
                        cattle.getApiRequestFilterDelegate().doFilter(request, response, chain);
                    } catch (IOException e) {
                        throw new WrappedException(e);
                    } catch (ServletException e) {
                        throw new WrappedException(e);
                    }
                }
            }.run();
        } catch (WrappedException e) {
            Throwable t = e.getCause();
            ExceptionUtils.rethrow(t, IOException.class);
            ExceptionUtils.rethrow(t, ServletException.class);
            ExceptionUtils.rethrowExpectedRuntime(t);
        }
    }

    protected void addUIHeaders(HttpServletRequest httpRequest, HttpServletResponse response) {
        response.setHeader(XFRAME, DENY);
    }

    protected void addVersionHeader(HttpServletRequest httpRequest, HttpServletResponse response) {
        response.setHeader(VERSION, SERVER_VERSION.get());
    }

    protected boolean isUIRequest(HttpServletRequest request, String path) {
        path = path.replaceAll("//+", "/");

        if ("/".equals(path)) {
            return RequestUtils.isBrowser(request, false);
        }

        boolean found = false;
        for (String version : cattle.getVersions().getVersions()) {
            if (path.startsWith("/" + version)) {
                found = true;
                break;
            }
        }

        return !found;
    }

    private static final class WrappedException extends RuntimeException {
        private static final long serialVersionUID = 8188803805854482331L;

        public WrappedException(Throwable cause) {
            super(cause);
        }
    }


    private void addPLCookie(HttpServletRequest httpRequest, HttpServletResponse response) {
        Cookie plCookie = null;
        if (httpRequest.getCookies() != null) {
            for (Cookie c : httpRequest.getCookies()) {
                if (PL.equals(c.getName()) && c.getName() != null) {
                    plCookie = c;
                    break;
                }
            }
        }

        if (plCookie == null || !PL_SETTING.getValue().equalsIgnoreCase(plCookie.getValue())) {
            plCookie = new Cookie(PL, PL_SETTING.getValue());
            plCookie.setPath("/");
            response.addCookie(plCookie);
        }
    }

    private void addDefaultLanguageCookie(HttpServletRequest httpRequest, HttpServletResponse response) {
        Cookie languageCookie = null;
        if(!StringUtils.isNotBlank(LOCALIZATION.get()))
            return;
        if(httpRequest.getCookies()!=null) {
            for(Cookie c : httpRequest.getCookies()) {
                if(LANG.equals(c.getName()) && c.getName()!=null) {
                    languageCookie = c;
                    break;
                    }
                }
        }
        if(languageCookie == null) {
            languageCookie = new Cookie(LANG, LOCALIZATION.get());
            languageCookie.setPath("/");
            response.addCookie(languageCookie);
        }
    }

    @Override
    public void destroy() {
    }

}