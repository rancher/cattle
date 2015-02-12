package io.cattle.platform.docker.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class DockerImageTest {

    @Test
    public void testNoNamespace() {
        DockerImage image = DockerImage.parse("ubuntu");
        assertEquals("index.docker.io",image.getServer());
        assertEquals("ubuntu", image.getRepository());
        assertEquals("latest", image.getTag());
        assertNull(image.getNamespace());
    }

    @Test
    public void testNoNamespaceWithTag() {
        DockerImage image = DockerImage.parse("ubuntu:14.04");
        assertEquals("index.docker.io",image.getServer());
        assertEquals("ubuntu", image.getRepository());
        assertEquals("14.04", image.getTag());
        assertNull(image.getNamespace());
    }

    @Test
    public void testNamespace() {
        DockerImage image = DockerImage.parse("ibuildthecloud/ubuntu-core");
        assertEquals("index.docker.io",image.getServer());
        assertEquals("ubuntu-core", image.getRepository());
        assertEquals("ibuildthecloud", image.getNamespace());
        assertEquals("latest", image.getTag());
    }

    @Test
    public void testTag() {
        DockerImage image = DockerImage.parse("ibuildthecloud/ubuntu-core:123");
        assertEquals("index.docker.io",image.getServer());
        assertEquals("ubuntu-core", image.getRepository());
        assertEquals("ibuildthecloud", image.getNamespace());
        assertEquals("123", image.getTag());
    }

    @Test
    public void testUrl() {
        DockerImage image = DockerImage.parse("quay.io/ibuildthecloud/ubuntu-core:123");
        assertEquals("quay.io",image.getServer());
        assertEquals("ubuntu-core", image.getRepository());
        assertEquals("ibuildthecloud", image.getNamespace());
        assertEquals("123", image.getTag());
    }

    @Test
    public void testUrlLatest() {
        DockerImage image = DockerImage.parse("quay.io/ibuildthecloud/ubuntu-core");
        assertEquals("quay.io",image.getServer());
        assertEquals("ubuntu-core", image.getRepository());
        assertEquals("ibuildthecloud", image.getNamespace());
        assertEquals("latest", image.getTag());
    }

    @Test
    public void testUrlTlnNoTag() {
        DockerImage image = DockerImage.parse("quay.io/ubuntu-core");
        assertEquals("quay.io",image.getServer());
        assertEquals("ubuntu-core", image.getRepository());
        assertNull(image.getNamespace());
        assertEquals("latest", image.getTag());
    }

    @Test
    public void testUrlTln() {
        DockerImage image = DockerImage.parse("quay.io/ubuntu-core:123");
        assertEquals("quay.io",image.getServer());
        assertEquals("ubuntu-core", image.getRepository());
        assertNull(image.getNamespace());
        assertEquals("123", image.getTag());
    }

    @Test
    public void testLocalHost() {
        DockerImage image = DockerImage.parse("localhost:5000/ibuildthecloud/ubuntu-core:123");
        assertEquals("localhost:5000",image.getServer());
        assertEquals("ubuntu-core", image.getRepository());
        assertEquals("ibuildthecloud", image.getNamespace());
        assertEquals("123", image.getTag());
    }

    @Test
    public void testLocalHostLatest() {
        DockerImage image = DockerImage.parse("localhost:5000/ibuildthecloud/ubuntu-core");
        assertEquals("localhost:5000",image.getServer());
        assertEquals("ubuntu-core", image.getRepository());
        assertEquals("ibuildthecloud", image.getNamespace());
        assertEquals("latest", image.getTag());
    }

    @Test
    public void testLocalHostTln() {
        DockerImage image = DockerImage.parse("localhost:5000/ubuntu-core:123");
        assertEquals("localhost:5000",image.getServer());
        assertEquals("ubuntu-core", image.getRepository());
        assertNull(image.getNamespace());
        assertEquals("123", image.getTag());
    }

    @Test
    public void testLocalHostTlnNoTag() {
        DockerImage image = DockerImage.parse("localhost:5000/ubuntu-core");
        assertEquals("localhost:5000",image.getServer());
        assertEquals("ubuntu-core", image.getRepository());
        assertNull(image.getNamespace());
        assertEquals("latest", image.getTag());
    }
    
    @Test
    public void testLocalHostTlnNoTagNoPort() {
        DockerImage image = DockerImage.parse("localhost/ubuntu-core");
        assertEquals("localhost",image.getServer());
        assertEquals("ubuntu-core", image.getRepository());
        assertNull(image.getNamespace());
        assertEquals("latest", image.getTag());
    }

    @Test
    public void testMyLocalHostTlnNoTagNoPort() {
        DockerImage image = DockerImage.parse("mylocalhost/ubuntu-core");
        assertEquals("index.docker.io",image.getServer());
        assertEquals("ubuntu-core", image.getRepository());
        assertEquals("mylocalhost", image.getNamespace());
        assertEquals("latest", image.getTag());
    }

    @Test
    public void testMalFormed() {
        DockerImage image = DockerImage.parse("foo:port/garbage/stuff/to:much");
        assertNull(image);
        image = DockerImage.parse("garbage:bar/foo:1:2:3");
        assertNull(image);
    }
}
