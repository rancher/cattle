package io.cattle.platform.object.serialization.impl;

import java.util.ArrayList;
import java.util.List;

public class Action {

    String name;
    List<Action> children = new ArrayList<Action>();

    public Action() {
    }

    public Action(String name, List<Action> children) {
        super();
        this.name = name;
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Action> getChildren() {
        return children;
    }

    public void setChildren(List<Action> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return name + (children.size() == 0 ? "" : children.toString());
    }
}
