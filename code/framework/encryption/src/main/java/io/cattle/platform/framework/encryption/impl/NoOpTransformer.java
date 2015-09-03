package io.cattle.platform.framework.encryption.impl;

import io.cattle.platform.framework.encryption.EncryptionUtils;
import io.github.ibuildthecloud.gdapi.model.Transformer;
import org.apache.commons.lang3.StringUtils;

public class NoOpTransformer implements Transformer {

    private static final String METADATA = "***";
    private static final String NAME = "NoOp";

    @Override
    public String transform(String value) {
        if (StringUtils.isBlank(value)){
            return "";
        }
        return METADATA + StringUtils.reverse(value) + StringUtils.reverse(METADATA);
    }

    @Override
    public String untransform(String value) {
        if (StringUtils.isBlank(value)){
            return "";
        }
        return StringUtils.reverse(StringUtils.removeEnd(StringUtils.removeStart(value, METADATA), StringUtils.reverse(METADATA)));
    }

    @Override
    public boolean compare(String plainText, String transformed) {
        return EncryptionUtils.isEqual(plainText, untransform(transformed));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init() {
    }
}
