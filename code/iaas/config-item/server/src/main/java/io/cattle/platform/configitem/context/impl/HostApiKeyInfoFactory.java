package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.ssh.common.SshKeyGen;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class HostApiKeyInfoFactory extends AbstractAgentBaseContextFactory {

    private static final Logger log = LoggerFactory.getLogger(HostApiKeyInfoFactory.class);

    HostApiService hostApiService;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        context.getData().putAll(getKeys());
    }

    protected Map<String, String> getKeys() {
        Map<String, String> result = new TreeMap<>();

        for (Map.Entry<String, PublicKey> entry : hostApiService.getPublicKeys().entrySet()) {
            String key = entry.getKey();
            String pem;
            try {
                pem = SshKeyGen.writePublicKey(entry.getValue());
                result.put("key_" + key, pem);
            } catch (Exception e) {
                log.error("Failed to write PEM", e);
            }
        }

        return result;
    }

    @Override
    public String getContentHash(String hash) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(hash.getBytes("UTF-8"));

            for (Map.Entry<String, String> entry : getKeys().entrySet()) {
                md.update(entry.getKey().getBytes("UTF-8"));
                md.update(entry.getValue().getBytes("UTF-8"));
            }

            hash = Hex.encodeHexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to find SHA-256", e);
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to find UTF-8", e);
        }

        return hash;
    }

    public HostApiService getHostApiService() {
        return hostApiService;
    }

    @Inject
    public void setHostApiService(HostApiService hostApiService) {
        this.hostApiService = hostApiService;
    }

}