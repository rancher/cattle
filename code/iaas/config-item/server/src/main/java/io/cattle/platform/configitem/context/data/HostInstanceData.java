package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;

public class HostInstanceData {

    IpAddress ipAddress;
    Nic nic;
    Subnet subnet;
    int mark;

    public Subnet getSubnet() {
        return subnet;
    }

    public void setSubnet(Subnet subnet) {
        this.subnet = subnet;
    }

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setNic(Nic nic) {
        this.nic = nic;
    }

    public Nic getNic() {
        return nic;
    }

    public int getMark() {
        return mark;
    }

    public void setMark(int mark) {
        this.mark = mark;
    }

}