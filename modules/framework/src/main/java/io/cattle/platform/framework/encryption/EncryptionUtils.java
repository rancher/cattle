package io.cattle.platform.framework.encryption;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicStringProperty;

public class EncryptionUtils {
    private static DynamicStringProperty fileName = ArchaiusUtil.getString("api.encryption.key");

    public static boolean isEqual(String aa, String bb) {
        if (aa == null || bb == null) {
            return false;
        }
        byte[] a = aa.getBytes();
        byte[] b = bb.getBytes();
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }


    public static byte[] getKeyFromFile(String propertyName) throws DecoderException {
        if (StringUtils.isBlank(propertyName)) {
            return null;
        }
        Properties keyStore = getKey();
        if (keyStore == null) {
            return null;
        }
        if (keyStore.get(propertyName) != null){
            String value = keyStore.get(propertyName).toString();
            return Hex.decodeHex(value.toCharArray());
        }
        return null;
    }

    private static Properties getKey() {
        String file = fileName.get();
        try (FileInputStream reader = new FileInputStream(file)) {
            Properties properties = new Properties();
            properties.load(reader);
            return properties;
        } catch (IOException e) {
            return null;
        }
    }

    public static synchronized void saveKeyToFile(String key, byte[] value) throws IOException {
        String file = fileName.get();
        try (FileOutputStream fileWriter = new FileOutputStream(file)){
            Properties properties = getKey();
            if (properties == null) {
                properties = new Properties();
            }
            properties.setProperty(key, Hex.encodeHexString(value));
            properties.store(fileWriter, null);
        }
    }
}
