package io.cattle.platform.agent.server.resource.impl;

import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class AgentResources {

    private static final Logger log = LoggerFactory.getLogger(AgentResources.class);
    private static final Set<String> IGNORE_FIELDS = CollectionUtils.set("localStorageMb");

    Map<String, Map<String, Object>> hosts = new TreeMap<>();
    Map<String, Map<String, Object>> storagePools = new TreeMap<>();
    Map<String, Map<String, Object>> ipAddresses = new TreeMap<>();
    String hash = null;

    public String getHash() {
        if (hash != null) {
            return hash;
        }

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to find SHA1 digest", e);
        }

        hash(md, hosts);
        hash(md, storagePools);
        hash(md, ipAddresses);

        return Hex.encodeHexString(md.digest());
    }

    public boolean hasContent() {
        return hosts.size() > 0;
    }

    protected void hash(MessageDigest md, Map<String, Map<String, Object>> data) {
        for (Map<String, Object> value : data.values()) {
            hashMap(md, value);
        }
    }

    protected void hashMap(MessageDigest md, Map<?, ?> data) {
        new TreeMap<>(data).forEach((key, value) -> {
            if (IGNORE_FIELDS.contains(key)) {
                return;
            }

            if (value instanceof String) {
                md.update(((String) value).getBytes(ObjectUtils.UTF8));
            } else if (value instanceof Map<?, ?>) {
                hashMap(md, (Map<?, ?>) value);
            }
        });
    }

    public Map<String, Map<String, Object>> getHosts() {
        return hosts;
    }

    public Map<String, Map<String, Object>> getStoragePools() {
        return storagePools;
    }

    public void setStoragePool(String uuid, Map<String, Object> data) {
        storagePools.put(uuid, new TreeMap<>(data));
    }

    public Map<String, Map<String, Object>> getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddress(String uuid, Map<String, Object> data) {
        ipAddresses.put(uuid, new TreeMap<>(data));
    }
}
