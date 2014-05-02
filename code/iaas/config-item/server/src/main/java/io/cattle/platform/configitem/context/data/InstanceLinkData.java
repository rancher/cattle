package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.IpAddress;

public class InstanceLinkData {

    InstanceLink link;
    IpAddress target;

    public InstanceLinkData() {
    }

    public InstanceLinkData(InstanceLink link, IpAddress target) {
        super();
        this.link = link;
        this.target = target;
    }

    public InstanceLink getLink() {
        return link;
    }

    public void setLink(InstanceLink link) {
        this.link = link;
    }

    public IpAddress getTarget() {
        return target;
    }

    public void setTarget(IpAddress target) {
        this.target = target;
    }

}
