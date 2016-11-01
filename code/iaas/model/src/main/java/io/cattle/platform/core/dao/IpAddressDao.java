package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;

import java.util.Map;

public interface IpAddressDao {

    IpAddress getPrimaryIpAddress(Nic nic);

    IpAddress mapNewIpAddress( Nic nic, Object key, Object... values);

    IpAddress assignAndActivateNewAddress(Host host, String ipAddress);

    IpAddress updateIpAddress(IpAddress ipAddress, String newIpAddress);

    IpAddress getInstancePrimaryIp(Instance instance);

    Map<Long, IpAddress> getNicIdToPrimaryIpAddress(long accountId);

}
