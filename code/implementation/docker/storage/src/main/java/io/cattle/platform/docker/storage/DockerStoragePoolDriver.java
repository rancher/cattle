package io.cattle.platform.docker.storage;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.docker.client.DockerImage;
import io.cattle.platform.docker.constants.DockerStoragePoolConstants;
import io.cattle.platform.storage.pool.AbstractKindBasedStoragePoolDriver;
import io.cattle.platform.storage.pool.StoragePoolDriver;

import java.util.HashMap;
import java.util.Map;

public class DockerStoragePoolDriver extends AbstractKindBasedStoragePoolDriver implements StoragePoolDriver {

    public DockerStoragePoolDriver() {
        super(DockerStoragePoolConstants.DOCKER_KIND);
    }

    @Override
    protected boolean populateImageInternal(String uuid, Image image) {
        DockerImage dockerImage = DockerImage.parse(uuid);

        if (dockerImage == null) {
            return false;
        }

        image.setName(dockerImage.getServer() + "/" + dockerImage.getFullName());

        Map<String, Object> data = image.getData();
        if (data == null) {
            data = new HashMap<>();
            image.setData(data);
        }

        data.put("dockerImage", dockerImage);
        image.setFormat(DockerStoragePoolConstants.DOCKER_FORMAT);
        image.setInstanceKind(InstanceConstants.KIND_CONTAINER);

        return true;
    }

    public static boolean isDockerPool(StoragePool pool) {
        return pool == null ? false : DockerStoragePoolConstants.DOCKER_KIND.equals(pool.getKind());
    }

}
