package io.cattle.platform.api.parser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class ProjectHttpServletRequest extends HttpServletRequestWrapper {

    private static final String PROJECT_HEADER = "X-API-Project-Id";

    private String projectId;
    private String servletPath;

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

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (PROJECT_HEADER.equalsIgnoreCase(name)) {
            return Collections.enumeration(Arrays.asList(projectId));
        } else {
            return super.getHeaders(name);
        }
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new HashSet<>(Collections.list(super.getHeaderNames()));
        names.add(PROJECT_HEADER);
        return Collections.enumeration(names);
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

}