package io.github.ibuildthecloud.gdapi.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

public class Action implements Serializable {

    private static final long serialVersionUID = -7616300411012161180L;

    String input, output;
    Map<String, Object> attributes = new HashMap<String, Object>();

    public Action() {
    }

    public Action(Action other) {
        this(other.getInput(), other.getOutput(), other.getAttributes());
    }

    public Action(String input, String output) {
        this(input, output, new HashMap<String, Object>());
    }

    public Action(String input, String output, Map<String, Object> attributes) {
        super();
        this.input = input;
        this.output = output;
        this.attributes = attributes;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    @XmlTransient
    @io.github.ibuildthecloud.gdapi.annotation.Field(include = false)
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @XmlTransient
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

}
