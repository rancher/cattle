package io.cattle.platform.object.meta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TypeSet {

    List<Class<?>> typeClasses = Collections.emptyList();
    List<String> typeNames = Collections.emptyList();

    public static TypeSet ofClasses(List<Class<?>> typeClasses) {
        TypeSet typeSet = new TypeSet();
        typeSet.setTypeClasses(typeClasses);
        return typeSet;
    }

    public static TypeSet ofClasses(Class<?>... typeClasses) {
        TypeSet typeSet = new TypeSet();
        typeSet.setTypeClasses(Arrays.asList(typeClasses));
        return typeSet;
    }

    public static TypeSet ofNames(String... typeNames) {
        TypeSet typeSet = new TypeSet();
        typeSet.setTypeNames(Arrays.asList(typeNames));
        return typeSet;
    }

    public List<Class<?>> getTypeClasses() {
        return typeClasses;
    }

    public void setTypeClasses(List<Class<?>> typeClasses) {
        this.typeClasses = typeClasses;
    }

    public List<String> getTypeNames() {
        return typeNames;
    }

    public void setTypeNames(List<String> typeNames) {
        this.typeNames = typeNames;
    }

}