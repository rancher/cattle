package io.cattle.platform.docker.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class DockerImageTest {

    @Test
    public void testNoNamespace() {
        DockerImage image = DockerImage.parse("ubuntu");
        assertEquals("ubuntu", image.getRepository());
        assertEquals("latest", image.getTag());
        assertNull(image.getNamespace());
    }

    @Test
    public void testNamespace() {
        DockerImage image = DockerImage.parse("ibuildthecloud/ubuntu-core");
        assertEquals("ubuntu-core", image.getRepository());
        assertEquals("ibuildthecloud", image.getNamespace());
        assertEquals("latest", image.getTag());
    }

    @Test
    public void testTag() {
        DockerImage image = DockerImage.parse("ibuildthecloud/ubuntu-core:123");
        assertEquals("ubuntu-core", image.getRepository());
        assertEquals("ibuildthecloud", image.getNamespace());
        assertEquals("123", image.getTag());
    }

}
