package io.cattle.platform.framework.encryption.impl;

import io.cattle.platform.framework.encryption.Encrypter;
import io.cattle.platform.framework.encryption.EncryptionUtils;

import java.security.Key;
import java.security.SecureRandom;
import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

import org.apache.commons.codec.binary.Hex;

public class Aes256Encrypter extends Encrypter {

    @Inject
    EncryptionUtils encryptionUtils;

    byte[] encryptionKeyBytes;

    private static final String NAME = "AES256";
    private Key key;
    private SecureRandom rn;

    @Override
    @PostConstruct
    public void init() {
        try {
            rn = new SecureRandom();

            encryptionKeyBytes = EncryptionUtils.getKeyFromFile(NAME + "Key");
            if (encryptionKeyBytes == null) {
                encryptionKeyBytes = new byte[32];
                rn.nextBytes(encryptionKeyBytes);
                EncryptionUtils.saveKeyToFile(NAME + "Key", encryptionKeyBytes);
            }
            key = new SecretKeySpec(encryptionKeyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Key generateKey() {
        byte[] encryptionKeyBytes = new byte[32];
        rn.nextBytes(encryptionKeyBytes);
        return new SecretKeySpec(encryptionKeyBytes, "AES");
    }

    @Override
    public String encrypt(String plainText) {
        return encrypt(plainText, key);
    }

    public String encrypt(String plainText, Key key) {
        try {
            Cipher encrypter = Cipher.getInstance("AES/CBC/PKCS5PADDING", "SunJCE");
            byte[] ivbytes = new byte[16];
            rn.nextBytes(ivbytes);
            String IV = new String(Hex.encodeHex(ivbytes));
            encrypter.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(ivbytes));
            return IV + ":" + String.valueOf(Hex.encodeHex(encrypter.doFinal(plainText.getBytes("UTF-8"))));
        } catch (Exception e){
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String decrypt(String value) {
        try {
            Cipher decrypter = Cipher.getInstance("AES/CBC/PKCS5PADDING", "SunJCE");
            String[] split = value.split(":", 2);
            decrypter.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(Hex.decodeHex(split[0].toCharArray())));
            return new String(decrypter.doFinal(Hex.decodeHex(split[1].toCharArray())), "UTF-8");
        } catch (Exception e){
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
