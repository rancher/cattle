package io.github.ibuildthecloud.dstack.docker.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.github.ibuildthecloud.dstack.core.model.Image;
import io.github.ibuildthecloud.dstack.core.model.StoragePool;
import io.github.ibuildthecloud.dstack.docker.client.DockerClient;
import io.github.ibuildthecloud.dstack.docker.client.DockerImage;
import io.github.ibuildthecloud.dstack.storage.pool.AbstractKindBasedStoragePoolDriver;
import io.github.ibuildthecloud.dstack.storage.pool.StoragePoolDriver;

public class DockerStoragePoolDriver extends AbstractKindBasedStoragePoolDriver implements StoragePoolDriver {

    public static final String DOCKER_KIND = "docker";


    DockerClient dockerClient;

    public DockerStoragePoolDriver() {
        super(DOCKER_KIND);
    }

    @Override
    protected boolean populateExtenalImageInternal(StoragePool pool, String uuid, Image image) throws IOException {
        DockerImage dockerImage = DockerImage.parse(stripKindPrefix(uuid));

        if ( dockerImage == null ) {
            return false;
        }

        dockerImage = dockerClient.lookup(dockerImage);

        if ( dockerImage == null ) {
            return false;
        }

        image.setName(dockerImage.getFullName());

        Map<String,Object> data = image.getData();
        if ( data == null ) {
            data = new HashMap<String, Object>();
            image.setData(data);
        }

        data.put("dockerImage", dockerImage);

        return true;
    }

    public DockerClient getDockerClient() {
        return dockerClient;
    }

    @Inject
    public void setDockerClient(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

}
