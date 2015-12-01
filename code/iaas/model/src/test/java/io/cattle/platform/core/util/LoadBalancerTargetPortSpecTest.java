package io.cattle.platform.core.util;

import static org.junit.Assert.assertEquals;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;

import org.junit.Test;

public class LoadBalancerTargetPortSpecTest {


    @Test(expected = ClientVisibleException.class)
    public void testInvalid() {
        new LoadBalancerTargetPortSpec("foo{");
    }

    @Test
    public void testValid() {
        LoadBalancerTargetPortSpec portSpec = new LoadBalancerTargetPortSpec("example.com{end}:80/path{beg}");
        assertEquals(portSpec.getHostCondition().name(), "end");
        assertEquals(portSpec.getPathCondition().name(), "beg");

        portSpec = new LoadBalancerTargetPortSpec("example.com{dir}:80/path{dom}=81");
        assertEquals(portSpec.getHostCondition().name(), "dir");
        assertEquals(portSpec.getPathCondition().name(), "dom");

        portSpec = new LoadBalancerTargetPortSpec("example.com{reg}:80/path/path/path{sub}");
        assertEquals(portSpec.getHostCondition().name(), "reg");
        assertEquals(portSpec.getPathCondition().name(), "sub");
    }
}
