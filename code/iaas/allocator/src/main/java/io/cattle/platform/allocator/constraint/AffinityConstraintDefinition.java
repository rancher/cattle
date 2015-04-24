package io.cattle.platform.allocator.constraint;

public class AffinityConstraintDefinition {
    public enum AffinityOps {
        SOFT_EQ("==~"),
        SOFT_NE("!=~"),
        EQ("=="),
        NE("!=");

        String symbol;

        private AffinityOps(String symbol) {
            this.symbol = symbol;
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
}
