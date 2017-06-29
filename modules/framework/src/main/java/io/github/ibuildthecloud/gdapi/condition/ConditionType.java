package io.github.ibuildthecloud.gdapi.condition;

public enum ConditionType {
    EQ, NE, LT, LTE, GT, GTE, PREFIX, LIKE, NOTLIKE, NULL, NOTNULL, IN(true), NOTIN(true), OR(true);

    public static final ConditionType[] NUMBER_MODS = new ConditionType[] { ConditionType.EQ, ConditionType.NE, ConditionType.LT, ConditionType.LTE,
            ConditionType.GT, ConditionType.GTE, ConditionType.NULL, ConditionType.NOTNULL };

    public static final ConditionType[] STRING_MODS = new ConditionType[] { ConditionType.EQ, ConditionType.NE, ConditionType.PREFIX, ConditionType.LIKE,
            ConditionType.NOTLIKE, ConditionType.NULL, ConditionType.NOTNULL };

    public static final ConditionType[] VALUE_MODS = new ConditionType[] { ConditionType.EQ, ConditionType.NE, ConditionType.NULL, ConditionType.NOTNULL };

    private String externalForm;
    private boolean internal = false;

    private ConditionType() {
        this.externalForm = toString().toLowerCase();
    }

    private ConditionType(boolean internal) {
        this();
        this.internal = internal;
    }

    public String getExternalForm() {
        return externalForm;
    }

    public boolean isInternal() {
        return internal;
    }

}
