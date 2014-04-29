package io.cattle.platform.resource.pool.mac;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

public class MacAddressGeneratorTest {

    protected Set<String> run(String start, String end) {
        Iterator<String> iter = new MacAddressGenerator(start, end);
        Set<String> ips = new LinkedHashSet<String>();

        while ( iter.hasNext() ) {
            ips.add(iter.next());
        }

        return ips;
    }

    @Test
    public void testSimpleRange() {
        Set<String> macs = run("00:02:fe", "00:03:01");

        assertEquals(4, macs.size());
        assertTrue(macs.contains("00:02:fe"));
        assertTrue(macs.contains("00:02:ff"));
        assertTrue(macs.contains("00:03:00"));
        assertTrue(macs.contains("00:03:01"));
    }
}
