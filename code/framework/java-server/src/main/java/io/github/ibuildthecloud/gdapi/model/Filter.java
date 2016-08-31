package io.github.ibuildthecloud.gdapi.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Filter implements Serializable {

    private static final long serialVersionUID = -4183989166167896474L;

    List<String> modifiers;

    public Filter() {
    }

    public Filter(Filter other) {
        this(new ArrayList<String>(other.getModifiers()));
    }

    public Filter(List<String> modifiers) {
        super();
        this.modifiers = modifiers;
    }

    public List<String> getModifiers() {
        return modifiers;
    }

    public void setModifiers(List<String> modifiers) {
        this.modifiers = modifiers;
    }
}
