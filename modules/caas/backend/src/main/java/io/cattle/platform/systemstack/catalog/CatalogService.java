package io.cattle.platform.systemstack.catalog;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.CatalogTemplate;
import io.cattle.platform.core.model.ProjectTemplate;
import io.cattle.platform.core.model.Stack;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.netflix.config.DynamicStringProperty;

public interface CatalogService {

    public static final DynamicStringProperty DEFAULT_TEMPLATE = ArchaiusUtil.getString("project.template.default.name");

    Map<String, CatalogTemplate> resolvedExternalIds(List<CatalogTemplate> templates) throws IOException;

    Stack deploy(Long accountId, CatalogTemplate template) throws IOException;

    Map<String, Map<Object, Object>> getTemplates(List<ProjectTemplate> installed) throws IOException;

    Map<String, String> latestInfraTemplates() throws IOException;

    String getTemplateIdFromExternalId(String externalId);

    Stack upgrade(Stack stack) throws IOException;

    String getDefaultExternalId(Stack stack) throws IOException;

}