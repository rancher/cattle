package io.cattle.platform.configitem.server.template;

import io.cattle.platform.configitem.server.resource.Resource;

import java.io.IOException;

public interface TemplateFactory {

    Template loadTemplate(Resource resource) throws IOException;

}
