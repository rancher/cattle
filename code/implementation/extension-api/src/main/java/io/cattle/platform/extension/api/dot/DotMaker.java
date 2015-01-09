package io.cattle.platform.extension.api.dot;

import io.cattle.platform.engine.process.ExtensionBasedProcessDefinition;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.StateTransition;
import io.cattle.platform.engine.process.StateTransition.Style;
import io.cattle.platform.engine.process.impl.ResourceStatesDefinition;
import io.cattle.platform.extension.ExtensionImplementation;
import io.cattle.platform.extension.ExtensionPoint;
import io.cattle.platform.extension.api.model.ProcessDefinitionApi;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;

public class DotMaker {

    List<ProcessDefinition> processDefinitions;
    String html;

    public String getResourceDot(String resourceType) {
        StringBuilder buffer = new StringBuilder();
        Set<String> nodes = new HashSet<String>();
        buffer.append("digraph \"").append(resourceType).append("\" {\n");

        for (ProcessDefinition def : processDefinitions) {
            if (ObjectUtils.equals(resourceType, def.getResourceType())) {
                addTransitions(def, nodes, buffer);
            }
        }

        buffer.append("}\n");
        return buffer.toString();
    }

    public boolean writeResponse(String dot, ApiRequest request) throws IOException {
        if (dot != null) {
            String content = getContent(request, dot);
            try {
                request.getOutputStream().write(content.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
            return true;
        }

        return false;
    }

    protected String getContent(ApiRequest request, String dot) {
        if ("html".equals(request.getResponseFormat())) {
            request.setResponseContentType("text/html; charset=utf-8");
            return asHtml(dot);
        } else {
            request.setResponseContentType("text/plain");
            return dot;
        }
    }

    protected String asHtml(String dot) {
        return html.replace("%DOT%", dot);
    }

    protected void addTransitions(ProcessDefinition def, Set<String> nodes, StringBuilder buffer) {
        String doneName = getName(def);
        String name = def.getName();
        String link = getLink(def);
        List<StateTransition> transitions = def.getStateTransitions();

        for (StateTransition transition : transitions) {
            String from = getTransitionName(transition, transition.getFromState());
            String to = getTransitionName(transition, transition.getToState());

            buffer.append("  \"").append(from).append("\" -> \"").append(to).append("\"");

            if (transition.getType() == Style.TRANSITIONING) {
                buffer.append(" [style=\"dotted\" label=\"" + name + "\" URL=\"" + link + "\"]");
            }

            if (transition.getType() == Style.DONE) {
                buffer.append(" [color=\"orange\" label=\"" + doneName + "\" URL=\"" + link + "\"]");
            }
            buffer.append(";\n");

            if (transition.getType() == Style.TRANSITIONING && !nodes.contains(to)) {
                buffer.append("  \"").append(to).append("\" [color=\"orange\"];\n");
                nodes.add(to);
            }
        }
    }

    protected String getTransitionName(StateTransition transition, String name) {
        if (ResourceStatesDefinition.DEFAULT_STATE_FIELD.equals(transition.getField())) {
            return name;
        } else {
            return transition.getField() + ":" + name;
        }
    }

    public String getProcessDot(ProcessDefinition def) {
        StringBuilder buffer = new StringBuilder();
        Set<String> nodes = new HashSet<String>();

        buffer.append("digraph \"").append(def.getName()).append("\" {\n");
        addTransitions(def, nodes, buffer);
        buffer.append("}\n");

        return buffer.toString();
    }

    protected String getName(ProcessDefinition def) {
        StringBuilder buffer = new StringBuilder(def.getName());
        buffer.append("[");

        if (def instanceof ExtensionBasedProcessDefinition) {
            int size = buffer.length();
            appendExtensionPoint(size, buffer, "pre", ((ExtensionBasedProcessDefinition) def).getPreProcessListenersExtensionPoint());
            appendExtensionPoint(size, buffer, "logic", ((ExtensionBasedProcessDefinition) def).getProcessHandlersExtensionPoint());
            appendExtensionPoint(size, buffer, "post", ((ExtensionBasedProcessDefinition) def).getPostProcessListenersExtensionPoint());

            if (buffer.length() == size) {
                buffer.append("no-op");
            }
            buffer.append("]  ");
        }

        String result = buffer.toString();
        if (!result.contains("no-op")) {
            result += "\" fontname=\"times-bold\" color=\"green\" style=\"bold";
        }

        return result;
    }

    protected String getLink(ProcessDefinition def) {
        return ApiContext.getUrlBuilder().resourceReferenceLink(ProcessDefinitionApi.class, def.getName()).toExternalForm();
    }

    protected void appendExtensionPoint(int size, StringBuilder buffer, String title, ExtensionPoint point) {
        if (point.getImplementations().size() == 0) {
            return;
        }

        if (buffer.length() > size) {
            buffer.append(",");
        }

        buffer.append(title).append("=");
        int count = 0;
        for (ExtensionImplementation impl : point.getImplementations()) {
            if (count > 0) {
                buffer.append(",");
            }
            buffer.append(impl.getName());
            count++;
        }
    }

    @PostConstruct
    public void init() throws IOException {
        InputStream is = null;
        try {
            is = getClass().getResourceAsStream("html-override.txt");
            if (is == null) {
                is = getClass().getResourceAsStream("html.txt");
            }

            if (is != null) {
                this.html = IOUtils.toString(is);
            }
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public List<ProcessDefinition> getProcessDefinitions() {
        return processDefinitions;
    }

    @Inject
    public void setProcessDefinitions(List<ProcessDefinition> processDefinitions) {
        this.processDefinitions = processDefinitions;
    }

}
