package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;

import javax.inject.Inject;
import javax.inject.Named;

import com.netflix.config.DynamicStringProperty;

@Named
public class SslSocatInfoFactory extends AbstractAgentBaseContextFactory {
    static final DynamicStringProperty CLUSTER_SECUREDOCKER_PORT = ArchaiusUtil.getString("cluster.securedocker.port");

    @Inject
    ObjectManager objectManager;

    @Override
    public String[] getItems() {
        return new String[] { "ssl-socat" };
    }

    @Override
    protected void populateContext(Agent agent, Instance instance,
            ConfigItem item, ArchiveContext context) {
        Host host = getHostFromAgent(agent);
        Long serverCertRecordId = host.getCertificateId();
        Certificate serverCerts = objectManager.loadResource(Certificate.class, serverCertRecordId);

        context.getData().put("serverKey", serverCerts.getKey());
        context.getData().put("serverCrt", serverCerts.getCert());
        context.getData().put("caCrt", serverCerts.getCertChain());
        context.getData().put("securePort", CLUSTER_SECUREDOCKER_PORT.get());
    }

    public Host getHostFromAgent(Agent agent) {
        // get cluster id from agent uri
        String uri = agent.getUri();
        String[] result = uri.split("hostId|&");
        Long hostId = Long.valueOf(result[1].substring(result[1].indexOf("=") + 1));
        return objectManager.loadResource(Host.class, hostId);
    }
}
