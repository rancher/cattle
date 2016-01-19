package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class RecreateOnQuorumStrategyConfig {
    private static Integer defaultQuorum = 1;

    Integer quorum;

    public RecreateOnQuorumStrategyConfig() {
    }

    public RecreateOnQuorumStrategyConfig(Integer quorum) {
        this.quorum = quorum;
    }

    @Field(nullable = false, required = true)
    public Integer getQuorum() {
        return quorum != null ? quorum : defaultQuorum;
    }

    public void setQuorum(Integer quorum) {
        this.quorum = quorum;
    }
}
