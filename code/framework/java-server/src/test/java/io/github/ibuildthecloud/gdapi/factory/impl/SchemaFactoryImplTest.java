package io.github.ibuildthecloud.gdapi.factory.impl;

import static org.junit.Assert.*;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.FieldType;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.testobject.TestType;
import io.github.ibuildthecloud.gdapi.testobject.TestTypeCRUD;
import io.github.ibuildthecloud.gdapi.testobject.TestTypeChild;
import io.github.ibuildthecloud.gdapi.testobject.TestTypeRename;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class SchemaFactoryImplTest {

    SchemaFactoryImpl factory;

    @Before
    public void setUp() {
        factory = new SchemaFactoryImpl();
    }

    protected Schema parseSchema(Class<?> clz) {
        Schema schema = factory.registerSchema(clz);
        return factory.parseSchema(schema.getId());
    }

    @Test
    public void testOrdering() {
        Schema schema = parseSchema(TestType.class);

        Iterator<String> fields = schema.getResourceFields().keySet().iterator();

        assertEquals("first", fields.next());
        assertEquals("second", fields.next());
        assertEquals("a", fields.next());
    }

    @Test
    public void testOnlyWriteable() {
        Schema schema = parseSchema(TestType.class);

        Iterator<String> fields = schema.getResourceFields().keySet().iterator();

        assertEquals("first", fields.next());
        assertEquals("second", fields.next());
        assertEquals("a", fields.next());
    }

    @Test
    public void testName() {
        Schema schema = parseSchema(TestType.class);

        assertEquals("testType", schema.getId());
        assertEquals("schema", schema.getType());
    }

    @Test
    public void testRename() {
        Schema schema = parseSchema(TestTypeRename.class);

        assertEquals("Renamed", schema.getId());
        assertEquals("schema", schema.getType());
    }

    @Test
    public void testSimpleTypes() {
        Schema schema = parseSchema(TestType.class);

        Map<String, Field> fields = schema.getResourceFields();

        assertEquals("blob", fields.get("typeBlob").getType());
        assertEquals("date", fields.get("typeDate").getType());
        assertEquals("enum", fields.get("typeEnum").getType());

        assertEquals("boolean", fields.get("typeBool").getType());
        assertTrue(!fields.get("typeBool").isNullable());
        assertEquals("boolean", fields.get("typeBoolean").getType());
        assertTrue(fields.get("typeBoolean").isNullable());

        assertEquals("float", fields.get("typeFloat").getType());
        assertTrue(!fields.get("typeFloat").isNullable());
        assertEquals("float", fields.get("typeFloatObject").getType());
        assertTrue(fields.get("typeFloatObject").isNullable());

        assertEquals("float", fields.get("typeDouble").getType());
        assertTrue(!fields.get("typeDouble").isNullable());
        assertEquals("float", fields.get("typeDoubleObject").getType());
        assertTrue(fields.get("typeDoubleObject").isNullable());

        assertEquals("int", fields.get("typeInt").getType());
        assertTrue(!fields.get("typeInt").isNullable());
        assertEquals("int", fields.get("typeInteger").getType());
        assertTrue(fields.get("typeInteger").isNullable());

        assertEquals("int", fields.get("typeLong").getType());
        assertTrue(!fields.get("typeLong").isNullable());
        assertEquals("int", fields.get("typeLongObject").getType());
        assertTrue(fields.get("typeLongObject").isNullable());

        assertEquals(FieldType.PASSWORD.getExternalType(), fields.get("typePassword").getType());
        assertEquals(FieldType.STRING.getExternalType(), fields.get("typeString").getType());

        assertEquals(FieldType.MAP, fields.get("typeMap").getTypeEnum());
        assertEquals(FieldType.REFERENCE, fields.get("typeReference").getTypeEnum());
        assertEquals(FieldType.ARRAY, fields.get("typeArray").getTypeEnum());
        assertEquals(FieldType.ARRAY, fields.get("typeList").getTypeEnum());

        assertEquals(FieldType.ARRAY, fields.get("testEnumList").getTypeEnum());
        assertEquals(FieldType.ENUM, fields.get("testEnumList").getSubTypeEnums().get(0));
    }

    @Test
    @Ignore
    public void testDefaultValue() {
        fail();
    }

    @Test
    public void testComplexType_Reference_Type() {
        parseSchema(TestTypeCRUD.class);
        parseSchema(TestTypeRename.class);
        Schema schema = parseSchema(TestType.class);

        Map<String, Field> fields = schema.getResourceFields();

        assertEquals("reference[testTypeCRUD]", fields.get("testTypeCrudId").getType());
        assertEquals("testTypeCRUD", fields.get("testTypeCrud").getType());
    }

    @Test
    public void testComplexType() {
        parseSchema(TestTypeCRUD.class);
        parseSchema(TestTypeRename.class);
        Schema schema = parseSchema(TestType.class);

        Map<String, Field> fields = schema.getResourceFields();

        assertEquals("array[map[string]]", fields.get("typeList").getType());
        assertEquals("array[string]", fields.get("typeArray").getType());
        assertEquals("array[testTypeCRUD]", fields.get("typeListCrud").getType());

        assertEquals("map[json]", fields.get("typeMapObject").getType());
    }

    @Test
    public void testDefaults() {
        Schema schema = parseSchema(TestType.class);

        Map<String, Field> fields = schema.getResourceFields();

        assertNull(fields.get("defaultSettings").getDefault());
        assertEquals("DEFAULT", fields.get("defaultValue").getDefault());
    }

    @Test
    public void testNullable() {
        Schema schema = parseSchema(TestType.class);

        Map<String, Field> fields = schema.getResourceFields();

        assertTrue(!fields.get("defaultSettings").isNullable());
        assertTrue(fields.get("nullable").isNullable());
    }

    @Test
    public void testUnique() {
        Schema schema = parseSchema(TestType.class);

        Map<String, Field> fields = schema.getResourceFields();

        assertTrue(!fields.get("defaultSettings").isUnique());
        assertTrue(fields.get("unique").isUnique());
    }

    @Test
    public void testValidChars() {
        Schema schema = parseSchema(TestType.class);

        Map<String, Field> fields = schema.getResourceFields();

        assertNull(fields.get("defaultSettings").getValidChars());
        assertEquals("valid", fields.get("validChars").getValidChars());
    }

    @Test
    public void testInvalidChars() {
        Schema schema = parseSchema(TestType.class);

        Map<String, Field> fields = schema.getResourceFields();

        assertNull(fields.get("defaultSettings").getInvalidChars());
        assertEquals("invalid", fields.get("invalidChars").getInvalidChars());
    }

    @Test
    public void testRequired() {
        Schema schema = parseSchema(TestType.class);

        Map<String, Field> fields = schema.getResourceFields();

        assertTrue(!fields.get("defaultSettings").isRequired());
        assertTrue(fields.get("required").isRequired());
    }

    @Test
    public void testCreateUpdate() {
        Schema schema = parseSchema(TestType.class);

        Map<String, Field> fields = schema.getResourceFields();

        assertTrue(!fields.get("defaultSettings").isCreate());
        assertTrue(!fields.get("defaultSettings").isUpdate());

        assertTrue(fields.get("createUpdate").isCreate());
        assertTrue(fields.get("createUpdate").isUpdate());

    }

    @Test
    public void testNameOverride() {
        Schema schema = parseSchema(TestType.class);

        Map<String, Field> fields = schema.getResourceFields();

        assertNull(fields.get("gonnaBeNameOverride"));
        assertNotNull(fields.get("nameOverride"));
    }

    @Test
    public void testLengths() {
        Schema schema = parseSchema(TestType.class);

        Map<String, Field> fields = schema.getResourceFields();

        assertNull(fields.get("defaultSettings").getMinLength());
        assertNull(fields.get("defaultSettings").getMaxLength());
        assertNull(fields.get("defaultSettings").getMin());
        assertNull(fields.get("defaultSettings").getMax());

        assertEquals(new Long(142), fields.get("lengths").getMinLength());
        assertEquals(new Long(242), fields.get("lengths").getMaxLength());
        assertEquals(new Long(342), fields.get("lengths").getMin());
        assertEquals(new Long(442), fields.get("lengths").getMax());
    }

    @Test
    public void testOptions() {
        Schema schema = parseSchema(TestType.class);
        Map<String, Field> fields = schema.getResourceFields();

        assertNull(fields.get("defaultSettings").getOptions());

        List<String> options = fields.get("typeEnum").getOptions();

        assertEquals(2, options.size());
        assertEquals("FIRST", options.get(0));
        assertEquals("SECOND", options.get(1));
    }

    @Test
    public void testTypeCRUD() {
        List<String> resourceMethods = parseSchema(TestType.class).getResourceMethods();
        List<String> collectionMethods = parseSchema(TestType.class).getCollectionMethods();

        assertEquals(1, resourceMethods.size());
        assertEquals(1, collectionMethods.size());

        assertEquals("GET", resourceMethods.get(0));
        assertEquals("GET", collectionMethods.get(0));

        resourceMethods = parseSchema(TestTypeCRUD.class).getResourceMethods();
        collectionMethods = parseSchema(TestTypeCRUD.class).getCollectionMethods();

        assertEquals(2, resourceMethods.size());
        assertTrue(resourceMethods.contains("DELETE"));
        assertTrue(resourceMethods.contains("PUT"));

        assertEquals(0, collectionMethods.size());
    }

    @Test
    public void testParentClass() {
        Schema parent = parseSchema(TestType.class);
        Schema child = parseSchema(TestTypeChild.class);

        assertEquals(parent.getId(), child.getParent());

        Map<String, Field> fields = child.getResourceFields();

        assertEquals("boolean", fields.get("typeBool").getType());
        assertTrue(!fields.get("typeBool").isNullable());
        assertEquals("boolean", fields.get("typeBoolean").getType());
        assertTrue(fields.get("typeBoolean").isNullable());
    }

    @Test
    public void testParentName() {
        Schema parent = parseSchema(TestType.class);

        Schema child = factory.registerSchema("child,parent=" + parent.getId());
        child = factory.parseSchema(child.getId());

        assertEquals(parent.getId(), child.getParent());

        Map<String, Field> fields = child.getResourceFields();

        assertEquals("boolean", fields.get("typeBool").getType());
        assertTrue(!fields.get("typeBool").isNullable());
        assertEquals("boolean", fields.get("typeBoolean").getType());
        assertTrue(fields.get("typeBoolean").isNullable());
    }

}