package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class InstanceHealthCheck {
    public enum Strategy {
        none,
        recreate,
        recreateOnQuorum
    }
    String name;
    Integer responseTimeout;
    Integer interval;
    Integer healthyThreshold;
    Integer unhealthyThreshold;
    String requestLine;
    Integer port;
    Strategy strategy;
    RecreateOnQuorumStrategyConfig recreateOnQuorumStrategyConfig;
    Integer initializingTimeout;
    Integer reinitializingTimeout;

    @Field(nullable = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getResponseTimeout() {
        return responseTimeout;
    }

    public void setResponseTimeout(Integer responseTimeout) {
        this.responseTimeout = responseTimeout;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Integer getHealthyThreshold() {
        return healthyThreshold;
    }

    public void setHealthyThreshold(Integer healthyThreshold) {
        this.healthyThreshold = healthyThreshold;
    }

    public Integer getUnhealthyThreshold() {
        return unhealthyThreshold;
    }

    public void setUnhealthyThreshold(Integer unhealthyThreshold) {
        this.unhealthyThreshold = unhealthyThreshold;
    }

    @Field(nullable = true)
    public String getRequestLine() {
        return requestLine;
    }

    public void setRequestLine(String requestLine) {
        this.requestLine = requestLine;
    }

    @Field(required = true, min = 1, max = 65535)
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Field(nullable = true, required = false)
    public RecreateOnQuorumStrategyConfig getRecreateOnQuorumStrategyConfig() {
        return recreateOnQuorumStrategyConfig;
    }

    public void setRecreateOnQuorumStrategyConfig(RecreateOnQuorumStrategyConfig recreateOnQuorumStrategyConfig) {
        this.recreateOnQuorumStrategyConfig = recreateOnQuorumStrategyConfig;
    }

    @Field(required = false, nullable = true, defaultValue = "recreate")
    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public Integer getInitializingTimeout() {
        return initializingTimeout;
    }

    public void setInitializingTimeout(Integer initializingTimeout) {
        this.initializingTimeout = initializingTimeout;
    }

    public Integer getReinitializingTimeout() {
        return reinitializingTimeout;
    }

    public void setReinitializingTimeout(Integer reinitializingTimeout) {
        this.reinitializingTimeout = reinitializingTimeout;
    }
}

