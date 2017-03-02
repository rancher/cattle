package io.cattle.platform.systemstack.model;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class Template {
    String id;
    String catalogId;
    String category;
    String defaultVersion;
    String defaultTemplateVersionId;
    String description;
    String license;
    String maintainer;
    String minimumRancherVersion;
    String name;
    String path;
    String type;
    String uuid;
    String version;

    Map<String, String> files;
    Map<String, String> versionLinks;
    Map<String, String> links;

    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getMaintainer() {
        return maintainer;
    }

    public void setMaintainer(String maintainer) {
        this.maintainer = maintainer;
    }

    public String getMinimumRancherVersion() {
        return minimumRancherVersion;
    }

    public void setMinimumRancherVersion(String minimumRancherVersion) {
        this.minimumRancherVersion = minimumRancherVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getVersionLinks() {
        return versionLinks;
    }

    public void setVersionLinks(Map<String, String> versionLinks) {
        this.versionLinks = versionLinks;
    }

    public String getDockerCompose() {
        String value = this.getFiles().get("docker-compose.yml");
        return StringUtils.isBlank(value) ? this.getFiles().get("docker-compose.yml.tpl") : value;
    }

    public String getRancherCompose() {
        return this.getFiles().get("rancher-compose.yml");
    }

    public Map<String, String> getFiles() {
        return files;
    }

    public void setFiles(Map<String, String> files) {
        this.files = files;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDefaultTemplateVersionId() {
        return defaultTemplateVersionId;
    }

    public void setDefaultTemplateVersionId(String defaultTemplateVersionId) {
        this.defaultTemplateVersionId = defaultTemplateVersionId;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }

}