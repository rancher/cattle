package io.cattle.platform.servicediscovery.api.action;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.addon.ComposeConfig;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class EnvironmentExportConfigActionHandler implements ActionHandler {
    @Inject
    ObjectManager objectManager;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ServiceDiscoveryService svcDiscoveryServer;

    @Override
    public String getName() {
        return ServiceDiscoveryConstants.PROCESS_ENV_EXPORT_CONFIG;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Environment)) {
            return null;
        }
        Environment env = (Environment) obj;
        List<? extends Long> serviceIds = DataAccessor.fromMap(request.getRequestObject())
                .withKey(ServiceDiscoveryConstants.FIELD_SERVICE_IDS).asList(jsonMapper, Long.class);


        List<? extends Service> services = objectManager.mappedChildren(env, Service.class);
        List<Service> toExport = new ArrayList<>();
        for (Service service : services) {
            // export only non-removed requested services
            if ((serviceIds == null || serviceIds.isEmpty()) || serviceIds.contains(service.getId())){
                if (service.getRemoved() == null && !service.getState().equals(CommonStatesConstants.REMOVED)) {
                    toExport.add(service);
                }
            }
        }
        SimpleEntry<String, String> composeConfig = svcDiscoveryServer.buildConfig(toExport);
        
        return new ComposeConfig(composeConfig.getKey(), composeConfig.getValue());
    }
}
