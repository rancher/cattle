package io.cattle.platform.systemstack.api;

import io.cattle.platform.core.addon.CatalogTemplate;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ProjectTemplateConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ProjectTemplate;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.systemstack.listener.SystemStackUpdate;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

public class AccountCreateFilter extends AbstractDefaultResourceManagerFilter {

    @Inject @Named("CoreSchemaFactory")
    SchemaFactory schemaFactory;
    @Inject
    ObjectManager objectManager;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Account account = request.proxyRequestObject(Account.class);
        ProjectTemplate template = objectManager.loadResource(ProjectTemplate.class, account.getProjectTemplateId());
        if (template == null) {
            return super.create(type, request, next);
        }

        List<CatalogTemplate> templates = DataAccessor.fieldObjectList(template, ProjectTemplateConstants.FIELD_STACKS, CatalogTemplate.class, jsonMapper);
        if (templates == null) {
            return super.create(type, request, next);
        }

        List<String> ids = new ArrayList<>();
        for (CatalogTemplate catalogTemplate : templates) {
            String id = catalogTemplate.getTemplateVersionId();
            if (StringUtils.isBlank(id)) {
                id = catalogTemplate.getTemplateId();
            }
            if (StringUtils.isNotBlank(id)) {
                ids.add(id);
            }
        }

        Map<String, Object> input = CollectionUtils.toMap(request.getRequestObject());
        input.put(AccountConstants.FIELD_ORCHESTRATION, SystemStackUpdate.chooseOrchestration(ids));

        return super.create(type, request, next);
    }

    @Override
    public String[] getTypes() {
        List<String> names =  schemaFactory.getSchemaNames(Account.class);
        return names.toArray(new String[names.size()]);
    }

}
