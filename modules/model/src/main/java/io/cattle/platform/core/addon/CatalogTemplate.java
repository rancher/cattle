package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.Map;

@Type(list = false)
public class CatalogTemplate {
    String name;
    String description;
    String templateId;
    String templateVersionId;
    String dockerCompose;
    String rancherCompose;
    Map<String, Object> answers;
    Object binding;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateVersionId() {
        return templateVersionId;
    }

    public void setTemplateVersionId(String templateVersionId) {
        this.templateVersionId = templateVersionId;
    }

    public String getDockerCompose() {
        return dockerCompose;
    }

    public void setDockerCompose(String dockerCompose) {
        this.dockerCompose = dockerCompose;
    }

    public String getRancherCompose() {
        return rancherCompose;
    }

    public void setRancherCompose(String rancherCompose) {
        this.rancherCompose = rancherCompose;
    }

    public Map<String, Object> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<String, Object> answers) {
        this.answers = answers;
    }

    public void setBinding(Object binding) {
        this.binding = binding;
    }

    @Field(typeString = "binding")
    public Object getBinding() {
        return binding;
    }

}