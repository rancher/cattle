package io.cattle.platform.iaas.api.auth.projects;

import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.LinkHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProjectMemberLinkHandler implements LinkHandler {

    ResourceManagerLocator managerLocator;

    public ProjectMemberLinkHandler(ResourceManagerLocator managerLocator) {
        this.managerLocator = managerLocator;
    }

    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return ProjectConstants.PROJECT_MEMBER.equalsIgnoreCase(link) && ProjectConstants.TYPE.equalsIgnoreCase(type);
    }

    @Override
    public Object link(String name, Object obj, ApiRequest request) throws IOException {
        Long projectId = ((Account) obj).getId();
        Map<String, String> criteria = request.getOptions();
        criteria.put("projectId", projectId.toString());
        ResourceManager rm = managerLocator.getResourceManagerByType("projectMember");
        return rm.list(request.getType(), new LinkedHashMap<>(criteria), new ListOptions(request));
    }
}
