package io.cattle.platform.network;

import io.cattle.platform.core.model.Network;

import java.util.Map;

public interface NetworkService {

    String getNetworkMode(Map<String, Object> instanceData);

    Network resolveNetwork(long accountId, String networkName);

    boolean shouldAssignIpAddress(Network network);

    IPAssignment assignIpAddress(Network network, Object owner, String requestedIp);

    void releaseIpAddress(Network network, Object owner);

}