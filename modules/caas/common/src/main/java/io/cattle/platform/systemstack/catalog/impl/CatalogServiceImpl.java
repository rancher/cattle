package io.cattle.platform.systemstack.catalog.impl;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.StackConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.systemstack.catalog.CatalogService;
import io.cattle.platform.systemstack.model.Template;
import io.cattle.platform.systemstack.model.TemplateCollection;
import io.cattle.platform.util.type.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CatalogServiceImpl implements CatalogService {

    private static final DynamicStringProperty CATALOG_VERSION_URL = ArchaiusUtil.getString("system.stack.catalog.versions.url");
    private static final DynamicStringProperty CATALOG_RESOURCE_URL = ArchaiusUtil.getString("system.stack.catalog.url");
    private static final DynamicStringProperty CATALOG_RESOURCE_VERSION = ArchaiusUtil.getString("rancher.server.version");

    JsonMapper jsonMapper;
    GenericResourceDao resourceDao;
    ObjectManager objectManager;
    ObjectProcessManager processManager;

    boolean firstCall = true;

    public CatalogServiceImpl(JsonMapper jsonMapper, GenericResourceDao resourceDao, ObjectManager objectManager, ObjectProcessManager processManager) {
        super();
        this.jsonMapper = jsonMapper;
        this.resourceDao = resourceDao;
        this.objectManager = objectManager;
        this.processManager = processManager;
    }

    protected void appendVersionCheck(StringBuilder catalogTemplateUrl) {
        String minVersion = CATALOG_RESOURCE_VERSION.get();
        if (StringUtils.isNotBlank(minVersion)) {
            catalogTemplateUrl.append("?rancherVersion=").append(minVersion);
        }
    }

    protected String getDefaultOrLatestTemplateExternalId(Template template) throws IOException {
        template = getDefaultOrLatestTemplate(template);
        return template == null ? null : String.format("catalog://%s", template.getId());
    }

    protected Template getDefaultOrLatestTemplate(Template template) throws IOException {
        if (template == null || template.getVersionLinks() == null) {
            return null;
        }

        return getTemplateVersionById(template.getDefaultTemplateVersionId());
    }

    protected Template getTemplateVersionById(String id) throws IOException {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        id = StringUtils.removeStart(id, "catalog://");

        StringBuilder catalogTemplateVersionUrl = new StringBuilder(CATALOG_VERSION_URL.get());
        catalogTemplateVersionUrl.append(id);
        appendVersionCheck(catalogTemplateVersionUrl);
        return getTemplateAtURL(catalogTemplateVersionUrl.toString());
    }

    protected Template getTemplateAtURL(String url) throws IOException {
        if (url == null) {
            return null;
        }

        return Request.Get(url).execute().handleResponse(response -> {
            if (response.getStatusLine().getStatusCode() != 200) {
                return null;
            }
            return jsonMapper.readValue(response.getEntity().getContent(), Template.class);
        });
    }

    @Override
    public String getDefaultExternalId(Stack stack) throws IOException {
        Template template = getDefaultTemplateVersion(stack);
        return template == null ? null : "catalog://" + template.getId();
    }

    protected Template getDefaultTemplateVersion(Stack stack) throws IOException {
        Template template = getTemplateVersionById(stack.getExternalId());
        if (template == null || template.getLinks() == null && !template.getLinks().containsKey("template")) {
            return null;
        }

        String url = template.getLinks().get("template");
        if (StringUtils.isBlank(url)) {
            return null;
        }
        StringBuilder builder = new StringBuilder(url);
        appendVersionCheck(builder);
        return getDefaultOrLatestTemplate(getTemplateAtURL(builder.toString()));
    }

    @Override
    public Stack upgrade(Stack stack) throws IOException {
        Template template = getDefaultTemplateVersion(stack);
        if (template == null) {
            return null;
        }

        processManager.update(stack, CollectionUtils.asMap(
                   StackConstants.FIELD_TEMPLATES, template.getFiles(),
                   StackConstants.FIELD_ANSWERS, DataAccessor.fieldMap(stack, StackConstants.FIELD_ANSWERS),
                   StackConstants.FIELD_EXTERNAL_ID, "catalog://" + template.getId()));

        return stack;
    }

    protected TemplateCollection getTemplates(String url) throws IOException {
        return Request.Get(url).execute().handleResponse(response -> {
            if (response.getStatusLine().getStatusCode() != 200) {
                return null;
            }
            return jsonMapper.readValue(response.getEntity().getContent(), TemplateCollection.class);
        });
    }

    protected void refresh() throws IOException {
        if (firstCall) {
            String url = CATALOG_RESOURCE_URL.get();
            if (url.endsWith("/")) {
                url = url.substring(0, url.length()-1);
            }
            Response resp = Request.Post(String.format("%s?refresh&action=refresh", url)).execute();
            int status = resp.returnResponse().getStatusLine().getStatusCode();
            if (status >= 400) {
                throw new IOException("Failed to reload got [" + status + "]");
            }
            firstCall = false;
        }
    }

    @Override
    public Map<String, String> latestInfraTemplates() throws IOException {
        refresh();

        Map<String, String> result = new HashMap<>();
        StringBuilder catalogTemplateUrl = new StringBuilder(CATALOG_RESOURCE_URL.get());
        appendVersionCheck(catalogTemplateUrl);
        if (catalogTemplateUrl.indexOf("?") == -1) {
            catalogTemplateUrl.append("?");
        } else {
            catalogTemplateUrl.append("&");
        }
        catalogTemplateUrl.append("templateBase_eq=infra");

        TemplateCollection collection = getTemplates(catalogTemplateUrl.toString());
        for (Template template : collection.getData()) {
            if (!"library".equals(template.getCatalogId())) {
                continue;
            }
            String externalId = getDefaultOrLatestTemplateExternalId(template);
            if (StringUtils.isNotBlank(externalId)) {
                result.put(template.getId(), externalId);
            }
        }

        return result;
    }

    @Override
    public String getTemplateIdFromExternalId(String externalId) {
        if (StringUtils.isBlank(externalId)) {
            return null;
        }

        String templateId = StringUtils.removeStart(externalId, "catalog://");
        String[] parts = StringUtils.split(templateId, ":", 3);
        if (parts.length < 3) {
            return null;
        }

        return String.format("%s:%s", parts[0], parts[1]);
    }

}
