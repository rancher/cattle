package io.github.ibuildthecloud.gdapi.condition;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.xml.bind.annotation.XmlTransient;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Condition {

    List<Object> values;
    ConditionType conditionType;
    Condition left, right;

    public static Condition ne(Object value) {
        return new Condition(ConditionType.NE, value);
    }

    public static Condition gt(Object value) {
        return new Condition(ConditionType.GT, value);
    }

    public static Condition gte(Object value) {
        return new Condition(ConditionType.GTE, value);
    }

    public static Condition in(Object... values) {
        return new Condition(ConditionType.IN, Arrays.asList(values));
    }

    public static Condition in(List<Object> values) {
        return new Condition(ConditionType.IN, values);
    }

    public static Condition like(Object value) {
        return new Condition(ConditionType.LIKE, value);
    }

    public static Condition lt(Object value) {
        return new Condition(ConditionType.LT, value);
    }

    public static Condition lte(Object value) {
        return new Condition(ConditionType.LTE, value);
    }

    public static Condition notIn(Object... values) {
        return new Condition(ConditionType.NOTIN, Arrays.asList(values));
    }

    public static Condition notLike(Object value) {
        return new Condition(ConditionType.NOTLIKE, value);
    }

    public static Condition isNotNull() {
        return new Condition(ConditionType.NOTNULL);
    }

    public static Condition isNull() {
        return new Condition(ConditionType.NULL);
    }

    public Condition or(Condition right) {
        return new Condition(this, right);
    }

    private Condition(Condition left, Condition right) {
        this(ConditionType.OR);
        this.left = left;
        this.right = right;
    }


    private Condition(ConditionType conditionType, List<Object> values) {
        this.values = values;
        this.conditionType = conditionType;
    }

    public Condition(ConditionType conditionType, Object value) {
        this.values = Arrays.asList(value);
        this.conditionType = conditionType;
    }

    private Condition(ConditionType conditionType) {
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

    public void setValue(Object value) {
        this.values = Collections.singletonList(value);
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
