package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;

public interface IpAddressDao {

    IpAddress getPrimaryIpAddress(Nic nic);

    IpAddress mapNewIpAddress(Nic nic, Object key, Object... values);

    IpAddress assignNewAddress(Host host, String ipAddress);

}
