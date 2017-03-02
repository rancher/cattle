package io.cattle.platform.systemstack.catalog.impl;

import static io.cattle.platform.core.model.tables.StackTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.CatalogTemplate;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.ProjectTemplate;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.systemstack.catalog.CatalogService;
import io.cattle.platform.systemstack.model.Template;
import io.cattle.platform.systemstack.model.TemplateCollection;
import io.cattle.platform.util.type.CollectionUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.yaml.snakeyaml.Yaml;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class CatalogServiceImpl implements CatalogService {

    private static DynamicStringProperty CATALOG_VERSION_URL = ArchaiusUtil.getString("system.stack.catalog.versions.url");
    private static DynamicStringProperty CATALOG_RESOURCE_URL = ArchaiusUtil.getString("system.stack.catalog.url");
    private static DynamicStringProperty CATALOG_RESOURCE_VERSION = ArchaiusUtil.getString("rancher.server.version");
    private static final DynamicBooleanProperty LAUNCH_CATALOG = ArchaiusUtil.getBoolean("catalog.execute");

    @Inject
    JsonMapper jsonMapper;

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager processManager;

    boolean firstCall = true;

    @Override
    public Map<String, CatalogTemplate> resolvedExternalIds(List<CatalogTemplate> templates) throws IOException {
        Map<String, CatalogTemplate> result = new HashMap<>();

        for (CatalogTemplate template : templates) {
            String externalId = resolveExternalId(template);
            if (StringUtils.isNotBlank(externalId)) {
                result.put(externalId, template);
            }
        }

        return result;
    }

    private String resolveExternalId(CatalogTemplate template) throws IOException {
        if (StringUtils.isNotBlank(template.getTemplateId()) || StringUtils.isNotBlank(template.getTemplateVersionId())) {
            return resolveUsingCatalog(template);
        }

        return DigestUtils.md5Hex(template.getDockerCompose() + template.getRancherCompose());
    }

    protected void appendVersionCheck(StringBuilder catalogTemplateUrl) {
        String minVersion = CATALOG_RESOURCE_VERSION.get();
        if (StringUtils.isNotBlank(minVersion)) {
            catalogTemplateUrl.append("?rancherVersion=").append(minVersion);
        }
    }

    private String resolveUsingCatalog(CatalogTemplate catalogTemplate) throws IOException {
        if (!LAUNCH_CATALOG.get()) {
            return null;
        }
        if (StringUtils.isNotBlank(catalogTemplate.getTemplateVersionId())) {
            return String.format("catalog://%s", catalogTemplate.getTemplateVersionId());
        }

        //get the latest version from the catalog template
        Template template = getTemplateById(catalogTemplate.getTemplateId());
        if (template == null || template.getVersionLinks() == null) {
            return null;
        }

        String versionUrl = null;
        String defaultVersionURL = template.getVersionLinks().get(template.getDefaultVersion());
        if (StringUtils.isNotBlank(defaultVersionURL)) {
            versionUrl = defaultVersionURL;
        } else {
            long maxVersion = 0;
            for (String url : template.getVersionLinks().values()) {
                long currentMaxVersion = Long.valueOf(url.substring(url.lastIndexOf(":") + 1, url.length()));
                if (currentMaxVersion >= maxVersion) {
                    maxVersion = currentMaxVersion;
                    versionUrl = url;
                }
            }
        }

        template = getTemplateAtURL(versionUrl);
        return template == null ? null : String.format("catalog://%s", template.getId());
    }

    protected Template getTemplateById(String id) throws IOException {
        StringBuilder catalogTemplateUrl = new StringBuilder(CATALOG_RESOURCE_URL.get());
        catalogTemplateUrl.append(id);
        appendVersionCheck(catalogTemplateUrl);
        return getTemplateAtURL(catalogTemplateUrl.toString());
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

        return Request.Get(url).execute().handleResponse(new ResponseHandler<Template>() {
            @Override
            public Template handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                if (response.getStatusLine().getStatusCode() != 200) {
                    return null;
                }
                return jsonMapper.readValue(response.getEntity().getContent(), Template.class);
            }
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

        template = getTemplateAtURL(template.getLinks().get("template"));
        if (template == null || StringUtils.isBlank(template.getDefaultTemplateVersionId())) {
            return null;
        }

        return getTemplateVersionById(template.getDefaultTemplateVersionId());
    }

    @Override
    public Stack upgrade(Stack stack) throws IOException {
        Template template = getDefaultTemplateVersion(stack);
        if (template == null) {
            return null;
        }

        processManager.scheduleProcessInstance(ServiceConstants.PROCESS_STACK_UPGRADE, stack,
                CollectionUtils.asMap(
                   ServiceConstants.STACK_FIELD_DOCKER_COMPOSE, template.getDockerCompose(),
                   ServiceConstants.STACK_FIELD_RANCHER_COMPOSE, template.getRancherCompose(),
                   ServiceConstants.STACK_FIELD_ENVIRONMENT, DataAccessor.fieldMap(stack, ServiceConstants.STACK_FIELD_ENVIRONMENT),
                   ServiceConstants.STACK_FIELD_EXTERNAL_ID, "catalog://" + template.getId()));

        return stack;
    }

    @Override
    public Stack deploy(Long accountId, CatalogTemplate catalogTemplate) throws IOException {
        String externalId = resolveExternalId(catalogTemplate);
        if (externalId == null) {
            return null;
        }

        String dockerCompose = catalogTemplate.getDockerCompose();
        String rancherCompose = catalogTemplate.getRancherCompose();
        Template template = null;
        if (externalId.startsWith("catalog://")) {
            template = getTemplateAtURL(CATALOG_RESOURCE_URL.get() + StringUtils.removeStart(externalId, "catalog://"));
            if (template != null) {
                dockerCompose = template.getDockerCompose();
                rancherCompose = template.getRancherCompose();
            }
        }

        Map<Object, Object> data = CollectionUtils.asMap(
                (Object)STACK.EXTERNAL_ID, externalId,
                STACK.ACCOUNT_ID, accountId,
                STACK.NAME, catalogTemplate.getName(),
                STACK.DESCRIPTION, catalogTemplate.getDescription(),
                STACK.SYSTEM, true,
                ServiceConstants.STACK_FIELD_DOCKER_COMPOSE, dockerCompose,
                ServiceConstants.STACK_FIELD_RANCHER_COMPOSE, rancherCompose,
                ServiceConstants.STACK_FIELD_ENVIRONMENT, catalogTemplate.getAnswers(),
                ServiceConstants.STACK_FIELD_BINDING, catalogTemplate.getBinding());
        return resourceDao.createAndSchedule(Stack.class, objectManager.convertToPropertiesFor(Stack.class, data));
    }

    protected TemplateCollection getTemplates(String url) throws IOException {
        return Request.Get(url).execute().handleResponse(new ResponseHandler<TemplateCollection>() {
            @Override
            public TemplateCollection handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                if (response.getStatusLine().getStatusCode() != 200) {
                    return null;
                }
                return jsonMapper.readValue(response.getEntity().getContent(), TemplateCollection.class);
            }
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
    public Map<String, Map<Object, Object>> getTemplates(List<ProjectTemplate> installed) throws IOException {
        refresh();

        Map<String, Map<Object, Object>> result = new HashMap<>();

        StringBuilder catalogTemplateUrl = new StringBuilder(CATALOG_RESOURCE_URL.get());
        appendVersionCheck(catalogTemplateUrl);
        if (catalogTemplateUrl.toString().contains("?")) {
            catalogTemplateUrl.append("&");
        } else {
            catalogTemplateUrl.append("?");
        }
        catalogTemplateUrl.append("templateBase_eq=project");

        TemplateCollection collection = getTemplates(catalogTemplateUrl.toString());

        Yaml yaml = new Yaml();
        if (collection.getData() != null) {
            for (Template template : collection.getData()) {
                if (template.getVersionLinks() == null) {
                    continue;
                }
                String url = template.getVersionLinks().get(template.getDefaultVersion());
                if (url == null) {
                    continue;
                }

                template = getTemplateAtURL(url);
                if (template == null || template.getFiles() == null) {
                    continue;
                }
                String file = template.getFiles().get("project.yml");
                if (file == null) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                Map<Object, Object> project = yaml.loadAs(file, Map.class);
                result.put("catalog://" + template.getId(), project);
            }
        }

        return result;
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
            if (!"library".equals(template.getCatalogId()) || StringUtils.isBlank(template.getDefaultVersion())) {
                continue;
            }
            if (StringUtils.isNotBlank(template.getDefaultTemplateVersionId())) {
                result.put(template.getId(), "catalog://" + template.getDefaultTemplateVersionId());
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
