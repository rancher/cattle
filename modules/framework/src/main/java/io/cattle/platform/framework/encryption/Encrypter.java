package io.cattle.platform.framework.encryption;

import io.cattle.platform.util.type.Named;
import io.github.ibuildthecloud.gdapi.model.Transformer;

import org.apache.commons.lang3.StringUtils;

public abstract class Encrypter implements Transformer, Named {

    @Override
    public boolean compare(String plainText, String encrypted) {
        return EncryptionUtils.isEqual(decrypt(encrypted), plainText);
    }

    @Override
    public String transform(String value){
        if (StringUtils.isBlank(value)){
            return "";
        }
        return encrypt(value);
    }

    @Override
    public String untransform(String value){
        if (StringUtils.isBlank(value)){
            return "";
        }
        return decrypt(value);
    }

    protected abstract String encrypt(String plainText);

    protected abstract String decrypt(String encrypted);
}
