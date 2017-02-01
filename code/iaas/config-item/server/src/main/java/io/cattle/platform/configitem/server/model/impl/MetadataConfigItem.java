package io.cattle.platform.configitem.server.model.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.context.impl.ServiceMetadataInfoFactory;
import io.cattle.platform.configitem.server.model.Request;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.client.DockerImage;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.netflix.config.DynamicStringProperty;

public class MetadataConfigItem extends AbstractConfigItem {

    public static final String ITEM = "metadata-answers";
    DynamicStringProperty METADATA_IMAGE_VERSION = ArchaiusUtil.getString("metadata.instance.image");

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

        // serve 404 to old metadata containers
        String defaultImageUUID = METADATA_IMAGE_VERSION.get();
        String imageUUID = DataAccessor.fieldString(instance, InstanceConstants.FIELD_IMAGE_UUID).toString();
        Pair<Long, Long> defaultMajorMinor = getMajorAndMinorVersion(defaultImageUUID);
        Pair<Long, Long> majorMinor = getMajorAndMinorVersion(imageUUID);
        
        boolean returnNothing = false;
        
        if (majorMinor.getLeft() == null || majorMinor.getRight() == null) {
            returnNothing = true;
        } else if (majorMinor.getLeft().longValue() <= majorMinor.getLeft().longValue()
                && majorMinor.getRight().longValue() < defaultMajorMinor.getRight()) {
            returnNothing = true;
        }

        if (returnNothing) {
            req.setResponseCode(Request.NOT_FOUND);
            return;
        }

        
        factory.writeMetadata(instance, getVersion(req), req.getOutputStream());
    }

    private Pair<Long, Long> getMajorAndMinorVersion(String imageUUID) {
        DockerImage dockerImage = DockerImage.parse(imageUUID);
        String[] splitted = dockerImage.getFullName().split("/");
        if (splitted.length < 2) {
            return Pair.of(null, null);
        }
        String server = splitted[1];
        // split the version
        String[] splittedServer = server.split(":");
        if (splittedServer.length < 2) {
            return Pair.of(null, null);
        }
        String version = splittedServer[splittedServer.length - 1];
        if (!version.startsWith("v")) {
            return Pair.of(null, null);
        }
        String[] splittedVersion = version.split("\\.");
        if (splittedVersion.length < 3) {
            return Pair.of(null, null);
        }
        Long major;
        Long minor;
        try {
            major = Long.parseLong(splittedVersion[0].substring(1));
            minor = Long.parseLong(splittedVersion[1]);
        } catch (NumberFormatException ex) {
            return Pair.of(null, null);
        }
        return Pair.of(major, minor);
    }

    @Override
    public String getSourceRevision() {
        return sourceRevision;
    }

}
