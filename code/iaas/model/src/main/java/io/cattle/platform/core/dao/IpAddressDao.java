package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.IpAssociation;
import io.cattle.platform.core.model.IpPool;
import io.cattle.platform.core.model.Nic;

public interface IpAddressDao {

    IpAddress getPrimaryIpAddress(Nic nic);

    IpAddress getPrimaryAssociatedIpAddress(IpAddress ipAddress);

    IpAddress mapNewIpAddress(Nic nic, Object key, Object... values);

    IpAddress assignNewAddress(Host host, String ipAddress);

    IpAddress assignAndActivateNewAddress(Host host, String ipAddress);

    IpAssociation createOrFindAssociation(IpAddress ip, IpAddress childIp);

    IpAddress createIpAddressFromPool(IpPool pool, Object key, Object... values);

}
