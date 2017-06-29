package io.github.ibuildthecloud.gdapi.condition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Condition {

    List<Object> values;
    ConditionType conditionType;
    Condition left, right;

    public Condition(Condition left, Condition right) {
        this(ConditionType.OR);
        this.left = left;
        this.right = right;
    }

    public Condition(ConditionType conditionType, List<Object> values) {
        this.values = values;
        this.conditionType = conditionType;
    }

    public Condition(ConditionType conditionType, Object value) {
        this.values = Arrays.asList(value);
        this.conditionType = conditionType;
    }

    public Condition(ConditionType conditionType) {
        super();
        this.values = Collections.emptyList();
        this.conditionType = conditionType;
    }

    @XmlTransient
    public List<Object> getValues() {
        return values;
    }

    public Object getValue() {
        return values.size() == 0 ? null : values.get(0);
    }

    @XmlTransient
    public ConditionType getConditionType() {
        return conditionType;
    }

    public String getModifier() {
        return conditionType.getExternalForm();
    }

    @XmlTransient
    public Condition getLeft() {
        return left;
    }

    @XmlTransient
    public Condition getRight() {
        return right;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("values", values)
                .append("conditionType", conditionType)
                .append("left", left)
                .append("right", right)
                .toString();
    }
}
