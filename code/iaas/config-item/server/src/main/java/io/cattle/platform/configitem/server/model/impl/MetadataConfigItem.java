package io.cattle.platform.configitem.server.model.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;

import io.cattle.platform.configitem.context.impl.ServiceMetadataInfoFactory;
import io.cattle.platform.configitem.registry.ConfigItemRegistry;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.Request;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataConfigItem extends AbstractConfigItem {

    private static final Logger log = LoggerFactory.getLogger(MetadataConfigItem.class);
    public static final String ITEM = "metadata-answers";

    ObjectManager objectManager;
    ServiceMetadataInfoFactory factory;
    String sourceRevision;
    ConfigItemRegistry itemRegistry;

    public MetadataConfigItem(ObjectManager objectManager, ServiceMetadataInfoFactory factory, ConfigItemStatusManager versionManager,
            ConfigItemRegistry itemRegistry) throws IOException {
        super(ITEM, versionManager);
        this.factory = factory;
        this.objectManager = objectManager;
        this.itemRegistry = itemRegistry;
        try(InputStream is = ServiceMetadataInfoFactory.class.getResourceAsStream(ServiceMetadataInfoFactory.class.getSimpleName() + ".class")) {
            sourceRevision = Hex.encodeHexString(DigestUtils.md5(is));
        }
    }

    @Override
    public void handleRequest(final Request req) throws IOException {
        Instance instance = objectManager.findAny(Instance.class,
                INSTANCE.AGENT_ID, req.getClient().getResourceId());
        if (instance == null) {
            return;
        }

        Object obj = req.getParams().get("client");
        if (!"v2".equals(RequestUtils.makeSingular(obj))) {
            req.setResponseCode(Request.NOT_FOUND);
            ConfigItem item = itemRegistry.getConfigItem(req.getItemName());
            if (item != null) {
                log.info("Setting item [{}] to latest for [{}]", req.getItemName(), req.getClient());
                req.setResponseCode(Request.NOT_FOUND);
                versionManager.setLatest(req.getClient(), req.getItemName(), item.getSourceRevision());
                return;
            }
            return;
        }

        factory.writeMetadata(instance, new Callable<String>() {
            @Override
            public String call() throws Exception {
                return getVersion(req);
            }
        }, req);
    }

    @Override
    public String getSourceRevision() {
        return sourceRevision;
    }

}
