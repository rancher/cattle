package io.cattle.platform.framework.encryption;

import io.cattle.platform.util.type.Named;
import io.github.ibuildthecloud.gdapi.model.Transformer;

import org.apache.commons.lang3.StringUtils;

public abstract class Hasher implements Transformer, Named {

    @Override
    public String transform(String value) {
        if (StringUtils.isBlank(value)){
            return "";
        }
        return hash(value);
    }

    protected abstract String hash(String value);

    @Override
    public String untransform(String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean compare(String plainText, String hashed) {
        return compareInternal(plainText, hashed);
    }

    protected abstract boolean compareInternal(String plainText, String hashed);
}
