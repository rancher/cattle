package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list=false)
public class StackConfiguration {
    String name;
    Object templates;
    String externalId;
    String project;
    Object answers;

    public StackConfiguration() {
    }

    public StackConfiguration(Object templates, String externalId, Object answers) {
        this.templates = templates;
        this.externalId = externalId;
        this.answers = answers;
    }

    @Field(typeString = "map[string]")
    public Object getTemplates() {
        return templates;
    }

    public void setTemplates(Object templates) {
        this.templates = templates;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @Field(typeString = "map[json]")
    public Object getAnswers() {
        return answers;
    }

    public void setAnswers(Object answers) {
        this.answers = answers;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
