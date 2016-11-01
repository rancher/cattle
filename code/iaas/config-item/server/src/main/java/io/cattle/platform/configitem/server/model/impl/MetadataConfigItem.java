package io.cattle.platform.configitem.server.model.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;

import io.cattle.platform.configitem.context.impl.ServiceMetadataInfoFactory;
import io.cattle.platform.configitem.server.model.Request;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

public class MetadataConfigItem extends AbstractConfigItem {

    public static final String ITEM = "metadata-answers";

    ObjectManager objectManager;
    ServiceMetadataInfoFactory factory;
    String sourceRevision;

    public MetadataConfigItem(ObjectManager objectManager, ServiceMetadataInfoFactory factory, ConfigItemStatusManager versionManager) throws IOException {
        super(ITEM, versionManager);
        this.factory = factory;
        this.objectManager = objectManager;
        try(InputStream is = ServiceMetadataInfoFactory.class.getResourceAsStream(ServiceMetadataInfoFactory.class.getSimpleName() + ".class")) {
            sourceRevision = Hex.encodeHexString(DigestUtils.md5(is));
        }
    }

    @Override
    public void handleRequest(Request req) throws IOException {
        Instance instance = objectManager.findAny(Instance.class,
                INSTANCE.AGENT_ID, req.getClient().getResourceId());
        if (instance == null) {
            return;
        }

        factory.writeMetadata(instance, getVersion(req), req.getOutputStream());
    }

    @Override
    public String getSourceRevision() {
        return sourceRevision;
    }

}
