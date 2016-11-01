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
import org.yaml.snakeyaml.Yaml;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class CatalogServiceImpl implements CatalogService {

    private static DynamicStringProperty CATALOG_RESOURCE_URL = ArchaiusUtil.getString("system.stack.catalog.url");
    private static DynamicStringProperty CATALOG_RESOURCE_VERSION = ArchaiusUtil.getString("rancher.server.version");
    private static final DynamicBooleanProperty LAUNCH_CATALOG = ArchaiusUtil.getBoolean("catalog.execute");

    @Inject
    JsonMapper jsonMapper;

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    ObjectManager objectManager;

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
            catalogTemplateUrl.append("?minimumRancherVersion_lte=").append(minVersion);
        }
    }

    private String resolveUsingCatalog(CatalogTemplate catalogTemplate) throws IOException {
        if (!LAUNCH_CATALOG.get()) {
            return null;
        }
        if (StringUtils.isNotBlank(catalogTemplate.getTemplateVersionId())) {
            return String.format("catalog://%s", catalogTemplate.getTemplateVersionId());
        }

        StringBuilder catalogTemplateUrl = new StringBuilder(CATALOG_RESOURCE_URL.get());
        catalogTemplateUrl.append(catalogTemplate.getTemplateId());
        appendVersionCheck(catalogTemplateUrl);

        //get the latest version from the catalog template
        Template template = getTemplate(catalogTemplateUrl.toString());
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

        template = getTemplate(versionUrl);
        return template == null ? null : String.format("catalog://%s", template.getId());
    }

    protected Template getTemplate(String url) throws IOException {
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
    public Stack deploy(Long accountId, CatalogTemplate catalogTemplate) throws IOException {
        String externalId = resolveExternalId(catalogTemplate);
        if (externalId == null) {
            return null;
        }

        String dockerCompose = catalogTemplate.getDockerCompose();
        String rancherCompose = catalogTemplate.getRancherCompose();
        Template template = null;
        if (externalId.startsWith("catalog://")) {
            template = getTemplate(CATALOG_RESOURCE_URL.get() + StringUtils.removeStart(externalId, "catalog://"));
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

    @Override
    public Map<String, Map<Object, Object>> getTemplates(List<ProjectTemplate> installed) throws IOException {
        Map<String, Map<Object, Object>> result = new HashMap<>();

        StringBuilder catalogTemplateUrl = new StringBuilder(CATALOG_RESOURCE_URL.get());
        appendVersionCheck(catalogTemplateUrl);
        if (catalogTemplateUrl.toString().contains("?")) {
            catalogTemplateUrl.append("&");
        } else {
            catalogTemplateUrl.append("?");
        }
        catalogTemplateUrl.append("templateBase_eq=project");

        TemplateCollection collection = Request.Get(catalogTemplateUrl.toString()).execute().handleResponse(new ResponseHandler<TemplateCollection>() {
            @Override
            public TemplateCollection handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                if (response.getStatusLine().getStatusCode() != 200) {
                    return null;
                }
                return jsonMapper.readValue(response.getEntity().getContent(), TemplateCollection.class);
            }
        });

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

                template = getTemplate(url);
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

}
