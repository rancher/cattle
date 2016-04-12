package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;

public interface HaConfigInput {

    @Field(defaultValue = "3", min=1, max=65535)
    int getClusterSize();

    @Field(defaultValue = "80", min=1, max=65535, nullable=true)
    int getHttpPort();

    @Field(defaultValue = "443", min=1, max=65535)
    int getHttpsPort();

    @Field(defaultValue = "81", min=1, max=65535, nullable=true)
    int getPpHttpPort();

    @Field(defaultValue = "444", min=1, max=65535)
    int getPpHttpsPort();

    @Field(defaultValue = "6379", min=1, max=65535)
    int getRedisPort();

    @Field(defaultValue = "2376", min=1, max=65535, nullable=true)
    int getSwarmPort();

    @Field(defaultValue = "2181", min=1, max=65535)
    int getZookeeperClientPort();

    @Field(defaultValue = "2888", min=1, max=65535)
    int getZookeeperQuorumPort();

    @Field(defaultValue = "3888", min=1, max=65535)
    int getZookeeperLeaderPort();

    @Field(nullable = true)
    String getCert();

    @Field(nullable = true)
    String getCertChain();

    @Field(nullable = true)
    String getKey();

    @Field(required = true)
    String getHostRegistrationUrl();

    @Field(defaultValue = "true")
    boolean getHttpEnabled();

    @Field(defaultValue = "true")
    boolean getSwarmEnabled();

}
