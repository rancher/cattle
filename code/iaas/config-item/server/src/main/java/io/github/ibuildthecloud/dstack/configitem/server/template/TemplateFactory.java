package io.github.ibuildthecloud.dstack.configitem.server.template;

import io.github.ibuildthecloud.dstack.configitem.server.resource.Resource;

import java.io.IOException;

public interface TemplateFactory {

    Template loadTemplate(Resource resource) throws IOException;

}
