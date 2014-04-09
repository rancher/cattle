package io.cattle.platform.core.util;

import static org.junit.Assert.*;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;

import org.junit.Test;

public class PortSpecTest {

    @Test
    public void testValidFormats() {
        PortSpec spec = new PortSpec("80");

        assertEquals(80, spec.getPrivatePort());
        assertEquals("tcp", spec.getProtocol());
        assertNull(spec.getPublicPort());

        spec = new PortSpec("81:80");
        assertEquals(80, spec.getPrivatePort());
        assertEquals("tcp", spec.getProtocol());
        assertEquals(new Integer(81), spec.getPublicPort());

        spec = new PortSpec("81:80/tcp");
        assertEquals(80, spec.getPrivatePort());
        assertEquals("tcp", spec.getProtocol());
        assertEquals(new Integer(81), spec.getPublicPort());

        spec = new PortSpec("81:80/udp");
        assertEquals(80, spec.getPrivatePort());
        assertEquals("udp", spec.getProtocol());
        assertEquals(new Integer(81), spec.getPublicPort());

        spec = new PortSpec("80/udp");
        assertEquals(80, spec.getPrivatePort());
        assertEquals("udp", spec.getProtocol());
        assertNull(spec.getPublicPort());
    }


    @Test
    public void testBadFormats() {
        try {
            new PortSpec("a");
            fail();
        } catch ( ClientVisibleException e ) {
            assertEquals(PortSpec.WRONG_FORMAT, e.getCode());
        }

        try {
            new PortSpec("0");
            fail();
        } catch ( ClientVisibleException e ) {
            assertEquals(PortSpec.INVALID_PRIVATE_PORT, e.getCode());
        }

        try {
            new PortSpec("65536");
            fail();
        } catch ( ClientVisibleException e ) {
            assertEquals(PortSpec.INVALID_PRIVATE_PORT, e.getCode());
        }

        try {
            new PortSpec("0:65535");
            fail();
        } catch ( ClientVisibleException e ) {
            assertEquals(PortSpec.INVALID_PUBLIC_PORT, e.getCode());
        }

        try {
            new PortSpec("65536:65535");
            fail();
        } catch ( ClientVisibleException e ) {
            assertEquals(PortSpec.INVALID_PUBLIC_PORT, e.getCode());
        }

        try {
            new PortSpec("80/asdf");
            fail();
        } catch ( ClientVisibleException e ) {
            assertEquals(PortSpec.INVALID_PROTOCOL, e.getCode());
        }
    }
}
