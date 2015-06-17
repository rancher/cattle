package io.cattle.platform.framework.encryption;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import com.netflix.config.DynamicStringProperty;

public class EncryptionConstants {
    public static final String ENCRYPTER_NAME_DELM = ":";
    public static final DynamicStringProperty ENCRYPTER_NAME = ArchaiusUtil.getString("api.encryption.encrypter");
    public static final DynamicStringProperty HASHER_NAME = ArchaiusUtil.getString("api.encryption.hasher");

    public static final String HASH = "hash";
    public static final String ENCRYPT = "encrypt";
}
