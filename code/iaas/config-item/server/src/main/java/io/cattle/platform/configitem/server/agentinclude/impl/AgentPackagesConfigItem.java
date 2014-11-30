package io.cattle.platform.configitem.server.agentinclude.impl;

import io.cattle.platform.configitem.context.ConfigItemContextFactory;
import io.cattle.platform.configitem.server.agentinclude.AgentIncludeMap;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.configitem.server.resource.ResourceRoot;
import io.cattle.platform.configitem.server.template.TemplateFactory;
import io.cattle.platform.configitem.server.template.TemplatesBasedArchiveItem;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;

public class AgentPackagesConfigItem extends TemplatesBasedArchiveItem {

    AgentIncludeMap map;

    public AgentPackagesConfigItem(String name, ConfigItemStatusManager versionManager, ResourceRoot resourceRoot,
            TemplateFactory templateFactory, AgentIncludeMap map) {
        super(name, versionManager, resourceRoot, templateFactory, Arrays.asList((ConfigItemContextFactory)new AgentPackagesContextFactory(name, map)));
        this.map = map;
    }

    @Override
    protected void writeContent(ArchiveContext context) throws IOException {
        writePackages(context);
        super.writeContent(context);
    }

    protected void writePackages(ArchiveContext context) throws IOException {
        StringBuilder buffer = new StringBuilder();

        for ( Map.Entry<String,String> entry : map.getMap(getName()).entrySet() ) {
            String value = isDevVersion(entry.getValue()) ? "config" : entry.getValue();
            buffer.append(entry.getKey()).append(" ").append(value).append("\n");
        }

        final byte[] content = buffer.toString().getBytes("UTF-8");
        withEntry(context, "packages", content.length, new WithEntry() {
            @Override
            public void with(OutputStream os) throws IOException {
                os.write(content);
            }
        });
    }

    @Override
    public String getSourceRevision() {
        return super.getSourceRevision() + map.getSourceRevision(getName());
    }

    public static final boolean isDevVersion(String value) {
        if ( value == null ) {
            return false;
        }

        return value.startsWith("/") || value.startsWith("\\") || value.startsWith(".");
    }

}
