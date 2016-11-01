package io.cattle.platform.configitem.server.model.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.server.model.Request;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.dao.DataDao;
import io.cattle.platform.core.dao.NicDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.object.ObjectManager;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.Callable;

import org.apache.commons.codec.binary.Hex;

import com.netflix.config.DynamicIntProperty;

public class PSKConfigItem extends AbstractConfigItem {

    private static final DynamicIntProperty LENGTH = ArchaiusUtil.getInt("ipsec.psk.byte.length");

    public static final String ITEM = "psk";

    SecureRandom random = new SecureRandom();
    String sourceRevision;
    NicDao nicDao;
    DataDao dataDao;
    ObjectManager objectManager;


    public PSKConfigItem(ObjectManager objectManager, NicDao nicDao, DataDao dataDao, ConfigItemStatusManager versionManager) throws IOException {
        super(ITEM, versionManager);
        this.sourceRevision = "";
        this.nicDao = nicDao;
        this.dataDao = dataDao;
        this.objectManager = objectManager;
    }

    protected String randomKey() {
        byte[] bytes = new byte[LENGTH.get()];
        random.nextBytes(bytes);
        return Hex.encodeHexString(bytes);
    }

    @Override
    public void handleRequest(Request req) throws IOException {
        Instance instance = objectManager.findAny(Instance.class,
                INSTANCE.AGENT_ID, req.getClient().getResourceId());
        if (instance == null) {
            return;
        }

        Nic primaryNic = nicDao.getPrimaryNic(instance);
        if (primaryNic == null) {
            return;
        }

        String key = dataDao.getOrCreate(String.format("psk.%d", primaryNic.getNetworkId()), false, new Callable<String>() {
            @Override
            public String call() throws Exception {
                return randomKey();
            }
        });

        req.getOutputStream().write(key.getBytes("UTF-8"));
    }

    @Override
    public String getSourceRevision() {
        return sourceRevision;
    }

}
