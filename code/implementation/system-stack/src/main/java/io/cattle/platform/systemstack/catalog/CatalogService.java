package io.cattle.platform.systemstack.catalog;

import io.cattle.platform.core.addon.CatalogTemplate;
import io.cattle.platform.core.model.ProjectTemplate;
import io.cattle.platform.core.model.Stack;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface CatalogService {

    Map<String, CatalogTemplate> resolvedExternalIds(List<CatalogTemplate> templates) throws IOException;

    Stack deploy(Long accountId, CatalogTemplate template) throws IOException;

    Map<String, Map<Object, Object>> getTemplates(List<ProjectTemplate> installed) throws IOException;

}