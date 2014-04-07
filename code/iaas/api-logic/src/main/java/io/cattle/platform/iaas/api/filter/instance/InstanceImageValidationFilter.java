package io.cattle.platform.iaas.api.filter.instance;

import javax.inject.Inject;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import com.netflix.config.DynamicBooleanProperty;

public class InstanceImageValidationFilter extends AbstractDefaultResourceManagerFilter {

    private static final DynamicBooleanProperty REQUIRE_INSTANCE_IMAGE = ArchaiusUtil.getBoolean("api.instance.require.image");

    ResourceManagerLocator locator;
    ObjectManager objectManager;
    AgentDao agentDao;

    @Override
    public String[] getTypes() {
        return new String[] { "container", "virtualMachine" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Instance.class };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Instance container = request.proxyRequestObject(Instance.class);

        if ( REQUIRE_INSTANCE_IMAGE.get() ) {
            Long imageId = container.getImageId();
            String imageUuid = DataUtils.getFieldFromRequest(request, InstanceConstants.FIELD_IMAGE_UUID, String.class);

            if ( imageId == null && imageUuid == null ) {
                throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED, InstanceConstants.FIELD_IMAGE_UUID);
            }
        }

        Long imageId = container.getImageId();
        Image image = getObjectManager().loadResource(Image.class, imageId);

        if ( image != null && image.getInstanceKind() != null ) {
            if ( ! image.getInstanceKind().equals(type) ) {
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidImageInstanceKind");
            }
        }

        return super.create(type, request, next);
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}