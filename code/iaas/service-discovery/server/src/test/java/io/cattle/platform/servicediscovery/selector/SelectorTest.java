package io.cattle.platform.servicediscovery.selector;

import static org.junit.Assert.assertEquals;
import io.cattle.platform.servicediscovery.api.util.selector.SelectorUtils;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SelectorTest {

    @Test
    public void testEq() {
        Map<String, String> labels = new HashMap<>();
        labels.put("foo", "bar");
        boolean result = SelectorUtils.isSelectorMatch("foo=bar", labels);
        assertEquals(result, true);

        // test with spaces
        result = SelectorUtils.isSelectorMatch("foo = bar", labels);
        assertEquals(result, true);

        labels.put("foo", "bar1");
        result = SelectorUtils.isSelectorMatch("foo=bar", labels);
        assertEquals(result, false);
    }

    @Test
    public void testNeq() {
        Map<String, String> labels = new HashMap<>();
        labels.put("foo", "bar");
        boolean result = SelectorUtils.isSelectorMatch("foo!=bar", labels);
        assertEquals(result, false);

        labels.put("foo", "bar1");
        result = SelectorUtils.isSelectorMatch("foo!=bar", labels);
        assertEquals(result, true);
    }

    @Test
    public void testIn() {
        Map<String, String> labels = new HashMap<>();
        labels.put("foo", "bar");
        labels.put("a", "b");
        boolean result = SelectorUtils.isSelectorMatch("foo in (bar,bar1)", labels);
        assertEquals(result, true);

        labels.put("foo", "bar2");
        labels.put("a", "b");
        result = SelectorUtils.isSelectorMatch("foo in (bar,bar1)", labels);
        assertEquals(result, false);
    }

    @Test
    public void testNotIn() {
        Map<String, String> labels = new HashMap<>();
        labels.put("foo", "bar");
        labels.put("a", "b");
        boolean result = SelectorUtils.isSelectorMatch("foo notin (bar,bar1)", labels);
        assertEquals(result, false);

        labels.put("foo", "bar2");
        labels.put("a", "b");
        result = SelectorUtils.isSelectorMatch("foo notin (bar,bar1)", labels);
        assertEquals(result, true);
    }

    @Test
    public void testList() {
        Map<String, String> labels = new HashMap<>();
        labels.put("foo", "inbar");
        labels.put("a", "b");
        boolean result = SelectorUtils.isSelectorMatch("foo in (inbar), a= b", labels);
        assertEquals(result, true);

        labels.put("foo", "bar2");
        labels.put("a", "b");
        result = SelectorUtils.isSelectorMatch("a =b1,foo in (bar2)", labels);
        assertEquals(result, false);
    }

    @Test
    public void testNoOp() {
        Map<String, String> labels = new HashMap<>();
        labels.put("foo", "bar");
        boolean result = SelectorUtils.isSelectorMatch("foo", labels);
        assertEquals(result, true);

        labels.clear();
        labels.put("foo1", "bar");
        result = SelectorUtils.isSelectorMatch("foo", labels);
        assertEquals(result, false);

        labels.clear();
        labels.put("foo", "bar");
        labels.put("bar", "foo");
        result = SelectorUtils.isSelectorMatch("foo, bar", labels);
        assertEquals(result, true);

        labels.clear();
        labels.put("foo", "bar");
        labels.put("bar1", "foo");
        result = SelectorUtils.isSelectorMatch("foo, bar", labels);
        assertEquals(result, false);
    }
}
