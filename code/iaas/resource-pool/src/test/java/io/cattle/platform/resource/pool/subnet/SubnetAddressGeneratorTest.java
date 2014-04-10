package io.cattle.platform.resource.pool.subnet;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

public class SubnetAddressGeneratorTest {

    protected Set<String> run(String start, String end) {
        Iterator<String> iter = new SubnetAddressGenerator(start, end);
        Set<String> ips = new LinkedHashSet<String>();

        while ( iter.hasNext() ) {
            ips.add(iter.next());
        }

        return ips;
    }

    @Test
    public void testSimpleRange() {
        Set<String> ips = run("192.168.0.1", "192.168.0.4");

        assertEquals(4, ips.size());
        assertTrue(ips.contains("192.168.0.1"));
        assertTrue(ips.contains("192.168.0.2"));
        assertTrue(ips.contains("192.168.0.3"));
        assertTrue(ips.contains("192.168.0.4"));
    }

    @Test
    public void testRolloverRange() {
        Set<String> ips = run("192.167.255.255", "192.168.0.1");

        assertEquals(3, ips.size());
        assertTrue(ips.contains("192.168.0.1"));
        assertTrue(ips.contains("192.167.255.255"));
        assertTrue(ips.contains("192.168.0.0"));
    }

    @Test
    public void testSingle() {
        Set<String> ips = run("192.168.0.1", "192.168.0.1");

        assertEquals(1, ips.size());
        assertTrue(ips.contains("192.168.0.1"));
    }

}
