package io.cattle.platform.ssh.common;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.sshd.common.util.SecurityUtils;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;

import com.netflix.config.DynamicStringProperty;

public class SshKeyGen {

    private static final byte[] HEADER = new byte[] {'s', 's', 'h', '-', 'r', 's', 'a'};
    private static final DynamicStringProperty SSH_FORMAT = ArchaiusUtil.getString("ssh.key.text.format");

    public static String[] generateKeys() throws Exception {
        KeyPair pair = generateKeyPair();

        String publicString = sshRsaTextFormat((RSAPublicKey)pair.getPublic());

        return new String[] { publicString, writeKeyPair(pair) };
    }

    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = SecurityUtils.getKeyPairGenerator("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    public static KeyPair readKeyPair(String key) throws Exception {
        SecurityUtils.isBouncyCastleRegistered();

        PEMReader r = null;
        try {
            if ( key.startsWith("---") ) {
                r = new PEMReader(new StringReader(key));
            } else {
                /* Backward compatibility with how the key was stored in data table */
                ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decodeBase64(key));
                r = new PEMReader(new InputStreamReader(bais));
            }
            return (KeyPair)r.readObject();
        } finally {
            IOUtils.closeQuietly(r);
        }
    }

    public static String writeKeyPair(KeyPair kp) throws Exception {
        SecurityUtils.isBouncyCastleRegistered();

        StringWriter stringWriter = new StringWriter();

        PEMWriter w = new PEMWriter(stringWriter);
        w.writeObject(kp);
        w.flush();
        IOUtils.closeQuietly(w);

        return stringWriter.toString();
    }

    public static String writePublicKey(PublicKey pk) throws Exception {
        SecurityUtils.isBouncyCastleRegistered();

        StringWriter stringWriter = new StringWriter();

        PEMWriter w = new PEMWriter(stringWriter);
        w.writeObject(pk);
        w.flush();
        IOUtils.closeQuietly(w);

        return stringWriter.toString();
    }

    public static String sshRsaTextFormat(RSAPublicKey key) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, HEADER);
        write(out, key.getPublicExponent().toByteArray());
        write(out, key.getModulus().toByteArray());

        return String.format(SSH_FORMAT.get(), Base64.encodeBase64String(out.toByteArray()));
    }

    protected static void write(OutputStream os, byte[] content) throws IOException {
        byte[] length = new byte[4];
        length[0] = (byte)((content.length >>> 24) & 0xff);
        length[1] = (byte)((content.length >>> 16) & 0xff);
        length[2] = (byte)((content.length >>> 8) & 0xff);
        length[3] = (byte)(content.length & 0xff);

        os.write(length);
        os.write(content);
    }

}
