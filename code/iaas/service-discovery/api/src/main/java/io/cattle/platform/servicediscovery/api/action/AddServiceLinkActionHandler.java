package io.cattle.platform.servicediscovery.api.action;

import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

public class AddServiceLinkActionHandler implements ActionHandler {

    JsonMapper jsonMapper;
    ServiceConsumeMapDao consumeMapDao;

    public AddServiceLinkActionHandler(JsonMapper jsonMapper, ServiceConsumeMapDao consumeMapDao) {
        super();
        this.jsonMapper = jsonMapper;
        this.consumeMapDao = consumeMapDao;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Service)) {
            return null;
        }
        Service service = (Service) obj;
        ServiceLink serviceLink = DataAccessor.fromMap(request.getRequestObject()).withKey(
                ServiceConstants.FIELD_SERVICE_LINK).as(jsonMapper, ServiceLink.class);

        consumeMapDao.createServiceLink(service, serviceLink);

        return service;
    }
}
