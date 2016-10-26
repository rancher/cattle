package io.cattle.platform.network;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;

public interface NetworkService {

    String getNetworkMode(Instance instance);

    Network resolveNetwork(long accountId, String networkName);

    boolean shouldAssignIpAddress(Network network);

    String assignIpAddress(Network network, Object owner, String requestedIp);

    void releaseIpAddress(Network network, Object owner);

}