package io.cattle.platform.allocator.constraint;

public class AffinityConstraintDefinition {
    public enum AffinityOps {
        SOFT_NE("!=~", "_soft_ne"),
        SOFT_EQ("==~", "_soft"),
        NE("!=", "_ne"),
        EQ("==", "");

        String envSymbol;
        String labelSymbol;

        private AffinityOps(String envSymbol, String labelSymbol) {
            this.envSymbol = envSymbol;
            this.labelSymbol = labelSymbol;
        }

        public String getEnvSymbol() {
            return envSymbol;
        }

        public String getLabelSymbol() {
            return labelSymbol;
        }
    }

    AffinityOps op;
    String key;
    String value;

    public AffinityConstraintDefinition(AffinityOps op, String key, String value) {
        this.op = op;
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }
}
