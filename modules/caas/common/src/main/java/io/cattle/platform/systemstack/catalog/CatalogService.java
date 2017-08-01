package io.cattle.platform.systemstack.catalog;

import io.cattle.platform.core.model.Stack;

import java.io.IOException;
import java.util.Map;

public interface CatalogService {

    Map<String, String> latestInfraTemplates() throws IOException;

    String getTemplateIdFromExternalId(String externalId);

    Stack upgrade(Stack stack) throws IOException;

    String getDefaultExternalId(Stack stack) throws IOException;

}