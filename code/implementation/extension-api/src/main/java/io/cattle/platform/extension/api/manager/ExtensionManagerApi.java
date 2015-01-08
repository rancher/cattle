package io.cattle.platform.extension.api.manager;

import io.cattle.platform.extension.ExtensionManager;
import io.cattle.platform.extension.ExtensionPoint;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.util.Map;

import javax.inject.Inject;

public class ExtensionManagerApi extends AbstractNoOpResourceManager {

    ExtensionManager extensionManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { ExtensionPoint.class };
    }

    @Override
    protected Resource createResource(Object obj, IdFormatter idFormatter, ApiRequest request) {
        if (obj instanceof ExtensionPoint) {
            return constructResource(idFormatter, request.getSchemaFactory(), request.getSchemaFactory().getSchema(ExtensionPoint.class), obj, request);
        }

        return super.createResource(obj, idFormatter, request);
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return extensionManager.getExtensions();
    }

    public ExtensionManager getExtensionManager() {
        return extensionManager;
    }

    @Inject
    public void setExtensionManager(ExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
    }

}
