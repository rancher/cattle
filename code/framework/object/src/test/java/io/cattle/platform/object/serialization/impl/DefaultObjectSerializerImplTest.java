package io.cattle.platform.object.serialization.impl;

import static org.junit.Assert.*;

import org.junit.Test;

public class DefaultObjectSerializerImplTest {

    DefaultObjectSerializerFactoryImpl serializer = new DefaultObjectSerializerFactoryImpl();

    @Test
    public void testSimpleList() {
        Action action = serializer.parseAction("type", "a,b,c");
        assertEquals("type[a, b, c]", action.toString());
        assertEquals("type", action.getName());
        assertEquals(3, action.getChildren().size());
        assertEquals(0, action.getChildren().get(0).getChildren().size());
        assertEquals("a", action.getChildren().get(0).getName());

        assertEquals(0, action.getChildren().get(1).getChildren().size());
        assertEquals("b", action.getChildren().get(1).getName());

        assertEquals(0, action.getChildren().get(2).getChildren().size());
        assertEquals("c", action.getChildren().get(2).getName());
    }

    @Test
    public void testDot() {
        Action action = serializer.parseAction("type", "a.x,b.y.z,c");
        assertEquals("type[a[x], b[y[z]], c]", action.toString());
        assertEquals("type", action.getName());
        assertEquals(3, action.getChildren().size());

        assertEquals("a", action.getChildren().get(0).getName());
        assertEquals(1, action.getChildren().get(0).getChildren().size());
        assertEquals("x", action.getChildren().get(0).getChildren().get(0).getName());

        assertEquals(1, action.getChildren().get(1).getChildren().size());
        assertEquals("b", action.getChildren().get(1).getName());
        assertEquals("y", action.getChildren().get(1).getChildren().get(0).getName());
        assertEquals(1, action.getChildren().get(1).getChildren().get(0).getChildren().size());
        assertEquals("z", action.getChildren().get(1).getChildren().get(0).getChildren().get(0).getName());

        assertEquals(0, action.getChildren().get(2).getChildren().size());
        assertEquals("c", action.getChildren().get(2).getName());
    }

    @Test
    public void testBrace() {
        Action action = serializer.parseAction("type", "a[x],b[y[z]],c");
        assertEquals("type[a[x], b[y[z]], c]", action.toString());

        action = serializer.parseAction("type", "a[x.y[1,2,c]],b[y[z]],c");
        assertEquals("type[a[x[y[1, 2, c]]], b[y[z]], c]", action.toString());
    }

}
