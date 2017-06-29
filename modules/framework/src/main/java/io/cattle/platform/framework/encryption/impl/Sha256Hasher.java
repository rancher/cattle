package io.cattle.platform.framework.encryption.impl;

import io.cattle.platform.framework.encryption.EncryptionUtils;
import io.cattle.platform.framework.encryption.Hasher;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.annotation.PostConstruct;

public class Sha256Hasher extends Hasher {

    private static final String NAME = "SHA256";

    private SecureRandom rn;

    @PostConstruct
    public void init() {
        rn = new SecureRandom();
    }

    @Override
    public String hash(String value) {
        byte[] saltBytes = new byte[10];
        rn.nextBytes(saltBytes);
        return hash256(value, bytesToHex(saltBytes));
    }

    @Override
    public boolean compareInternal(String plainText, String previousHash) {
        String[] split = previousHash.split(":");
        if (split.length != 2){
            return false;
        }
        String hashed = hash256(plainText, split[0]);
        return EncryptionUtils.isEqual(previousHash, hashed);
    }

    @Override
    public String getName() {
        return NAME;
    }

    private String hash256(String data, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes("UTF-8"));
            String hashed = bytesToHex(md.digest(data.getBytes("UTF-8")));
            return salt + ":" + hashed;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte byt : bytes) result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }
}
