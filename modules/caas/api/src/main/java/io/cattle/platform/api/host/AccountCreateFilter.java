package io.cattle.platform.api.host;

import io.cattle.platform.core.addon.CatalogTemplate;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ProjectTemplateConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ProjectTemplate;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AccountCreateFilter extends AbstractValidationFilter {

    ObjectManager objectManager;
    JsonMapper jsonMapper;

    public AccountCreateFilter(ObjectManager objectManager, JsonMapper jsonMapper) {
        super();
        this.objectManager = objectManager;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Account account = request.proxyRequestObject(Account.class);
        ProjectTemplate template = objectManager.loadResource(ProjectTemplate.class, account.getProjectTemplateId());
        if (template == null) {
            return super.create(type, request, next);
        }

        List<CatalogTemplate> templates = DataAccessor.fieldObjectList(template, ProjectTemplateConstants.FIELD_STACKS, CatalogTemplate.class);
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
        input.put(AccountConstants.FIELD_ORCHESTRATION, AccountConstants.chooseOrchestration(ids));

        return super.create(type, request, next);
    }

}
