package io.cattle.platform.framework.encryption.impl;

import io.cattle.platform.framework.encryption.EncryptionUtils;
import io.cattle.platform.framework.encryption.Hasher;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class PasswordHasher extends Hasher {

    private static final String NAME = "PBKDF2";

    private SecureRandom rn;

    @Override
    @PostConstruct
    public void init() {
        rn = new SecureRandom();
    }

    @Override
    public String hash(String value) {
        byte[] saltBytes = new byte[10];
        rn.nextBytes(saltBytes);
        return pbkdf2(1000, saltBytes, value.toCharArray());
    }

    @Override
    public boolean compareInternal(String plainText, String previousHash) {
        String[] split = previousHash.split(":");
        if (split.length != 3){
            return false;
        }
        String hashed;
        try {
            hashed = pbkdf2(Integer.parseInt(split[0]), Hex.decodeHex(split[1].toCharArray()), plainText.toCharArray());
        } catch (NumberFormatException | DecoderException e) {
            throw new RuntimeException("Failed to decode", e);
        }
        return EncryptionUtils.isEqual(previousHash, hashed);
    }

    @Override
    public String getName() {
        return NAME;
    }

    private String pbkdf2(int iterations, byte[] salt, char[] data) {
        try {
            PBEKeySpec spec = new PBEKeySpec(data, salt, iterations, 64 * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return iterations + ":" + Hex.encodeHexString(salt) + ":" + Hex.encodeHexString(hash);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
