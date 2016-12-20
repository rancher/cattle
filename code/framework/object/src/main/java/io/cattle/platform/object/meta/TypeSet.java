package io.cattle.platform.object.meta;

import io.cattle.platform.util.type.Named;
import io.cattle.platform.util.type.Priority;

import java.util.Collections;
import java.util.List;

public class TypeSet implements Priority, Named {

    List<Class<?>> typeClasses = Collections.emptyList();
    List<String> typeNames = Collections.emptyList();
    int priority = Priority.DEFAULT;
    String name;

    public TypeSet(String name) {
        this.name = name;
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

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}