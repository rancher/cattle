package org.apache.cloudstack.configitem.server.impl;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import io.cattle.platform.configitem.model.DefaultItemVersion;
import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.registry.impl.ConfigItemRegistryImpl;
import io.cattle.platform.configitem.server.impl.ConfigItemServerImpl;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;

import org.apache.cloudstack.configitem.server.model.impl.TestRequest;
import org.apache.cloudstack.configitem.server.model.impl.WriteStringConfigItem;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ConfigItemServerImplTest {

    ConfigItemServerImpl server;
    ConfigItemRegistryImpl registry;
    TestRequest req;

    @Mock
    ConfigItemStatusManager versionManager = null;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        registry = new ConfigItemRegistryImpl();

        server = new ConfigItemServerImpl();
        server.setVersionManager(versionManager);
        server.setItemRegistry(registry);
        req = new TestRequest();
    }

    @Test
    public void test_not_found() throws Exception {
        req.setItemName("missing");
        server.handleRequest(req);

        assertEquals(404, req.getResponseCode());
        assertEquals("", req.getResponseContent());
    }

    @Test
    @Ignore
    public void test_write_string() throws Exception {
        registry.register(new WriteStringConfigItem("string", "content"));
        req.setItemName("string");
        server.handleRequest(req);

        assertEquals(200, req.getResponseCode());
        assertEquals("content", req.getResponseContent());
    }

    @Test
    @Ignore
    public void test_applied() throws Exception {
        ArgumentCaptor<ItemVersion> itemVersion = ArgumentCaptor.forClass(ItemVersion.class);
        DefaultItemVersion version = DefaultItemVersion.fromString("000000042-ok");

        req.setAppliedVersion(version);
        server.handleRequest(req);

        verify(versionManager).setApplied(eq(req.getClient()), eq("testitem"), itemVersion.capture());

        assertEquals(42, itemVersion.getValue().getRevision());
        assertEquals("ok", itemVersion.getValue().getSourceRevision());
        assertEquals(200, req.getResponseCode());
    }

    @Test
    @Ignore
    public void test_applied_latest() throws Exception {
        DefaultItemVersion version = DefaultItemVersion.fromString("latest");

        req.setAppliedVersion(version);
        req.setItemName("name1");
        registry.register(new WriteStringConfigItem("name1", "content1"));
        server.handleRequest(req);

        assertTrue(version.isLatest());
        verify(versionManager).setLatest(eq(req.getClient()), eq("name1"), eq("name1/content1"));
        assertEquals(200, req.getResponseCode());
    }

    @Test
    public void test_applied_latest_not_found() throws Exception {
        DefaultItemVersion version = DefaultItemVersion.fromString("latest");

        req.setAppliedVersion(version);
        req.setItemName("name1");
        server.handleRequest(req);

        assertEquals(404, req.getResponseCode());
    }

}
