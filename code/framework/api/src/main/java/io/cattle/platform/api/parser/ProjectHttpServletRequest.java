package io.cattle.platform.api.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class ProjectHttpServletRequest extends HttpServletRequestWrapper {

    public static final String PROJECT_HEADER = "X-API-Project-Id";

    String projectId;
    String servletPath;

    public ProjectHttpServletRequest(HttpServletRequest request, String projectId, String servletPath) {
        super(request);
        this.projectId = projectId;
        this.servletPath = servletPath;
    }

    @Override
    public String getHeader(String name) {
        if (PROJECT_HEADER.equalsIgnoreCase(name)) {
            return projectId;
        } else {
            return super.getHeader(name);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getHeaders(String name) {
        if (PROJECT_HEADER.equalsIgnoreCase(name)) {
            return Collections.enumeration(Arrays.asList(projectId));
        } else {
            return super.getHeaders(name);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Enumeration getHeaderNames() {
        Set names = new HashSet(Collections.list(super.getHeaderNames()));
        names.add(PROJECT_HEADER);
        return Collections.enumeration(names);
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

}