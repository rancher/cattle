package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class RecreateOnQuorumStrategyConfig {
    Integer quorum;

    public RecreateOnQuorumStrategyConfig() {
    }

    public RecreateOnQuorumStrategyConfig(Integer quorum) {
        this.quorum = quorum;
    }

    @Field(nullable = false, required = true)
    public Integer getQuorum() {
        return quorum;
    }

    public void setQuorum(Integer quorum) {
        this.quorum = quorum;
    }
}
