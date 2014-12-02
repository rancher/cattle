package io.cattle.platform.configitem.context.impl;

import java.security.PublicKey;
import java.util.Map;

import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.ssh.common.SshKeyGen;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class HostApiKeyInfoFactory extends AbstractAgentBaseContextFactory {

    private static final Logger log = LoggerFactory.getLogger(HostApiKeyInfoFactory.class);

    HostApiService hostApiService;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        for ( Map.Entry<String, PublicKey> entry : hostApiService.getPublicKeys().entrySet() ) {
            String key = entry.getKey();
            String pem;
            try {
                pem = SshKeyGen.writePublicKey(entry.getValue());
                context.getData().put("key_" + key, pem);
            } catch (Exception e) {
                log.error("Failed to write PEM", e);
            }
        }
    }

    public HostApiService getHostApiService() {
        return hostApiService;
    }

    @Inject
    public void setHostApiService(HostApiService hostApiService) {
        this.hostApiService = hostApiService;
    }

}
