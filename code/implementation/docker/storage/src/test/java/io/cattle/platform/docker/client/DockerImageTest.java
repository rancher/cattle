package io.cattle.platform.docker.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class DockerImageTest {

    private static final String sha = "sha256:88f8d82bc9bc20ff80992cdeeee1dd6d8799cd36797b3653c644943e90b3acdf";

    @Test
    public void testNoNamespace() {
        DockerImage image = DockerImage.parse("ubuntu");
        assertEquals("index.docker.io", image.getServer());
    }

    @Test
    public void testNoNamespaceWithTag() {
        DockerImage image = DockerImage.parse("ubuntu:14.04");
        assertEquals("index.docker.io", image.getServer());
    }

    @Test
    public void testNamespace() {
        DockerImage image = DockerImage.parse("ibuildthecloud/ubuntu-core");
        assertEquals("index.docker.io", image.getServer());
    }

    @Test
    public void testTag() {
        DockerImage image = DockerImage.parse("ibuildthecloud/ubuntu-core:123");
        assertEquals("index.docker.io", image.getServer());
    }

    @Test
    public void testUrl() {
        DockerImage image = DockerImage.parse("quay.io/ibuildthecloud/ubuntu-core:123");
        assertEquals("quay.io", image.getServer());
    }

    @Test
    public void testUrlLatest() {
        DockerImage image = DockerImage.parse("quay.io/ibuildthecloud/ubuntu-core");
        assertEquals("quay.io", image.getServer());
    }

    @Test
    public void testUrlTlnNoTag() {
        DockerImage image = DockerImage.parse("quay.io/ubuntu-core");
        assertEquals("quay.io", image.getServer());
    }

    @Test
    public void testUrlTln() {
        DockerImage image = DockerImage.parse("quay.io/ubuntu-core:123");
        assertEquals("quay.io", image.getServer());
    }

    @Test
    public void testLocalHost() {
        DockerImage image = DockerImage.parse("localhost:5000/ibuildthecloud/ubuntu-core:123");
        assertEquals("localhost:5000", image.getServer());
    }

    @Test
    public void testLocalHostLatest() {
        DockerImage image = DockerImage.parse("localhost:5000/ibuildthecloud/ubuntu-core");
        assertEquals("localhost:5000", image.getServer());
    }

    @Test
    public void testLocalHostTln() {
        DockerImage image = DockerImage.parse("localhost:5000/ubuntu-core:123");
        assertEquals("localhost:5000", image.getServer());
    }

    @Test
    public void testLocalHostTlnNoTag() {
        DockerImage image = DockerImage.parse("localhost:5000/ubuntu-core");
        assertEquals("localhost:5000", image.getServer());
    }

    @Test
    public void testLocalHostTlnNoTagNoPort() {
        DockerImage image = DockerImage.parse("localhost/ubuntu-core");
        assertEquals("localhost", image.getServer());
    }

    @Test
    public void testMyLocalHostTlnNoTagNoPort() {
        DockerImage image = DockerImage.parse("mylocalhost/ubuntu-core");
        assertEquals("index.docker.io", image.getServer());
    }

    @Test
    public void testMalFormed() {
        DockerImage image = DockerImage.parse("foo:port/garbage/stuff/to:much");
        assertNull(image);
        image = DockerImage.parse("garbage:bar/foo:1:2:3");
        assertNull(image);
    }

    @Test
    public void testNoNamespaceWithShaTag() {
        DockerImage image = DockerImage.parse("ubuntu@" + sha);
        assertEquals("index.docker.io", image.getServer());
    }

    @Test
    public void testShaTag() {
        DockerImage image = DockerImage.parse("ibuildthecloud/ubuntu-core@" + sha);
        assertEquals("index.docker.io", image.getServer());
    }

    @Test
    public void testUrlSha() {
        DockerImage image = DockerImage.parse("quay.io/ibuildthecloud/ubuntu-core@" + sha);
        assertEquals("quay.io", image.getServer());
    }

    @Test
    public void testUrlTlnSha() {
        DockerImage image = DockerImage.parse("quay.io/ubuntu-core@" + sha);
        assertEquals("quay.io", image.getServer());
    }

    @Test
    public void testLocalHostSha() {
        DockerImage image = DockerImage.parse("localhost:5000/ibuildthecloud/ubuntu-core@" + sha);
        assertEquals("localhost:5000", image.getServer());
    }

    @Test
    public void testLocalHostTlnSha() {
        DockerImage image = DockerImage.parse("localhost:5000/ubuntu-core@" + sha);
        assertEquals("localhost:5000", image.getServer());
    }
}
