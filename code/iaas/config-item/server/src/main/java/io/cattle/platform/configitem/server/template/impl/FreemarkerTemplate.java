package io.cattle.platform.configitem.server.template.impl;

import io.cattle.platform.configitem.server.resource.Resource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FreemarkerTemplate implements io.cattle.platform.configitem.server.template.Template {

    private static final Logger log = LoggerFactory.getLogger(FreemarkerTemplate.class);

    String outputName;
    Template template;
    Resource resource;

    public FreemarkerTemplate(String outputName, Template template, Resource resource) {
        super();
        this.outputName = outputName;
        this.template = template;
        this.resource = resource;
    }

    @Override
    public String getOutputName() {
        return outputName;
    }

    @Override
    public void execute(Map<String, Object> context, OutputStream os) throws IOException {
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(os);
            template.process(context, writer);
        } catch (TemplateException e) {
            log.error("Failed to run template for [{}]", resource.getName(), e);
            throw new IOException(e);
        } finally {
            writer.flush();
        }
    }

    @Override
    public long getSize() {
        return -1;
    }

}
