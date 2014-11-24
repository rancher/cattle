package io.cattle.platform.configitem.server.agentinclude.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.server.agentinclude.AgentIncludeMap;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.binary.Hex;

import com.netflix.config.DynamicListProperty;
import com.netflix.config.DynamicStringProperty;

public class AgentIncludeMapImpl implements AgentIncludeMap {

    private static final DynamicListProperty<String> KEYS = ArchaiusUtil.getList("agent.packages.types");

    Map<String, DynamicStringProperty> values = new ConcurrentHashMap<String, DynamicStringProperty>();

    @Override
    public List<String> getNamedMaps() {
        return KEYS.get();
    }

    @Override
    public Map<String,String> getMap(String name) {
        Map<String,String> result = new LinkedHashMap<String, String>();

        if ( name == null ) {
            return result;
        }

        for ( String item : ArchaiusUtil.getList("agent.packages." + name).get() ) {
            String key = String.format("agent.package.%s.url", item);
            DynamicStringProperty prop = values.get(key);

            if ( prop == null ) {
                prop = ArchaiusUtil.getString(key);
                values.put(key, prop);
            }

            String value = prop.get();

            if ( value != null ) {
                result.put(item, value);
            }
        }

        return result;
    }

    @Override
    public String getSourceRevision(String name) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            for ( Map.Entry<String, String> entry : getMap(name).entrySet() ) {
                md.update(entry.getKey().getBytes("UTF-8"));
                md.update(entry.getValue().getBytes("UTF-8"));
            }

            return Hex.encodeHexString(md.digest());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}