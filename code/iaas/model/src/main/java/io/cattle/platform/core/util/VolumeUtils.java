package io.cattle.platform.core.util;

import org.apache.commons.codec.digest.DigestUtils;

public class VolumeUtils {

    public static String externalId(String externalId) {
        if (externalId == null) {
            return null;
        }

        if (externalId.length() < 128) {
            return externalId;
        }

        return DigestUtils.md5Hex(externalId);
    }

}
