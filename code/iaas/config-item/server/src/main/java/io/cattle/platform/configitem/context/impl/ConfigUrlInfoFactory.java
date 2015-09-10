package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.server.context.ServerContext.BaseProtocol;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;

import javax.inject.Named;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ConfigUrlInfoFactory extends AbstractAgentBaseContextFactory implements Callable<byte[]> {

    private static final Logger log = LoggerFactory.getLogger(ConfigUrlInfoFactory.class);

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        context.getData().put("customApiHost", ServerContext.isCustomApiHost());
        context.getData().put("configUrl", ServerContext.getHostApiBaseUrl(BaseProtocol.HTTP));
    }

    @Override
    public String getContentHash(String hash) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(call());
            hash = Hex.encodeHexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to find SHA-256", e);
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to find UTF-8", e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return hash;
    }

    @Override
    public byte[] call() throws Exception {
        return ServerContext.getHostApiBaseUrl(BaseProtocol.HTTP).getBytes("UTF-8");
    }

}