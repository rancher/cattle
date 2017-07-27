package io.cattle.platform.systemstack.catalog;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.CatalogTemplate;
import io.cattle.platform.core.model.ProjectTemplate;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.systemstack.model.Template;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface CatalogService {

    boolean isEnabled();

    DynamicStringProperty DEFAULT_TEMPLATE = ArchaiusUtil.getString("project.template.default.name");

    Map<String, CatalogTemplate> resolvedExternalIds(List<CatalogTemplate> templates) throws IOException;

    Stack deploy(Long accountId, CatalogTemplate template) throws IOException;

    Map<String, Map<Object, Object>> getTemplates(List<ProjectTemplate> installed) throws IOException;

    Map<String, String> latestInfraTemplates() throws IOException;

    String getTemplateIdFromExternalId(String externalId);

    Stack upgrade(Stack stack) throws IOException;

    String getDefaultExternalId(Stack stack) throws IOException;

    Template lookupTemplate(String id) throws IOException;

    String getTemplateBase(Template template) throws IOException;

    String getTemplateBase(String externalId) throws IOException;


}