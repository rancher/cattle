package io.cattle.platform.ssh.common;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.crypto.KeyAgreement;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicStringProperty;

public class SshKeyGen {

    private static final byte[] HEADER = new byte[] { 's', 's', 'h', '-', 'r', 's', 'a' };
    private static final DynamicStringProperty SSH_FORMAT = ArchaiusUtil.getString("ssh.key.text.format");
    private static final DynamicLongProperty EXPIRATION = ArchaiusUtil.getLong("cert.expiry.days");
    private static final JcaPEMKeyConverter CONVERTER = new JcaPEMKeyConverter().setProvider("BC");
    private static final Random RANDOM = new Random();
    public static final String BOUNCY_CASTLE = "BC";

    static {
        if (java.security.Security.getProvider(BOUNCY_CASTLE) == null) {
            java.security.Security.addProvider(new BouncyCastleProvider());
            try {
                MessageDigest.getInstance("MD5", BOUNCY_CASTLE);
                KeyAgreement.getInstance("DH", BOUNCY_CASTLE);
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String[] generateKeys() throws Exception {
        KeyPair pair = generateKeyPair();

        String publicString = sshRsaTextFormat((RSAPublicKey) pair.getPublic());

        return new String[] { publicString, toPEM(pair) };
    }

    public static X509Certificate createRootCACert(KeyPair keyPair) throws Exception {
        X509v3CertificateBuilder certBldr = new JcaX509v3CertificateBuilder(
                new X500Name("O=cattle"),
                BigInteger.valueOf(Math.abs(RANDOM.nextLong())),
                new Date(System .currentTimeMillis()),
                new Date(System.currentTimeMillis() + EXPIRATION.get() * 24 * 60 * 60 * 1000),
                new X500Name("O=cattle"), keyPair.getPublic());
        certBldr.addExtension(Extension.basicConstraints,
                              true, new BasicConstraints(true))
                .addExtension(Extension.keyUsage,
                              true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

        ContentSigner signer = new JcaContentSignerBuilder("SHA512withRSA").setProvider("BC").build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certBldr.build(signer));
    }

    public static X509Certificate generateClientCert(String subject, PublicKey entityKey, PrivateKey caKey,
            X509Certificate caCert, String... sans) throws NoSuchAlgorithmException, CertIOException, OperatorCreationException, CertificateException {
        X509v3CertificateBuilder certBldr = new JcaX509v3CertificateBuilder(
                caCert.getSubjectX500Principal(),
                BigInteger.valueOf(Math.abs(RANDOM.nextLong())),
                new Date(System.currentTimeMillis()),
                new Date(System.currentTimeMillis() + EXPIRATION.get() * 24 * 60 * 60 * 1000),
                new X500Principal("CN=" + subject),
                entityKey);


        List<GeneralName> sanNameList = new ArrayList<>();
        for (String san : sans) {
            if (san.startsWith("IP:")) {
                sanNameList.add(new GeneralName(GeneralName.iPAddress, san.substring(3)));
                sanNameList.add(new GeneralName(GeneralName.dNSName, san.substring(3)));
            } else {
                sanNameList.add(new GeneralName(GeneralName.dNSName, san));
            }
        }

        GeneralName[] sanNames = sanNameList.toArray(new GeneralName[sanNameList.size()]);

        certBldr.addExtension(Extension.subjectAlternativeName,
                              false, new GeneralNames(sanNames))
                .addExtension(Extension.basicConstraints,
                              true, new BasicConstraints(false))
                .addExtension(Extension.keyUsage,
                              true, new KeyUsage(KeyUsage.digitalSignature
                                               | KeyUsage.keyEncipherment))
                .addExtension(Extension.extendedKeyUsage,
                              true, ExtendedKeyUsage.getInstance(new DERSequence(new ASN1Encodable[]{
                                      KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth
                                      }))
                              );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(caKey);
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certBldr.build(signer));
    }

    public static KeyPair generateKeyPair(int keySize) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", BOUNCY_CASTLE);
        generator.initialize(keySize);
        return generator.generateKeyPair();
    }

    public static KeyPair generateKeyPair() throws Exception {
        return generateKeyPair(2048);
    }

    public static X509Certificate readCACert(String encoded) throws Exception {
        try (PEMParser r = new PEMParser(new StringReader(encoded))) {
            X509CertificateHolder holder = ((X509CertificateHolder) r.readObject());
            return new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
        }
    }

    public static KeyPair readKeyPair(String key) throws Exception {
        PEMParser r = null;
        try {
            if (key.startsWith("---")) {
                r = new PEMParser(new StringReader(key));
            } else {
                /*
                 * Backward compatibility with how the key was stored in data
                 * table
                 */
                ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decodeBase64(key));
                r = new PEMParser(new InputStreamReader(bais));
            }
            PEMKeyPair kp = (PEMKeyPair) r.readObject();
            return CONVERTER.getKeyPair(kp);
        } finally {
            IOUtils.closeQuietly(r);
        }
    }

    public static String toPEM(Object obj) throws Exception {
        StringWriter stringWriter = new StringWriter();

        try (JcaPEMWriter w = new JcaPEMWriter(stringWriter)) {
            w.writeObject(obj);
            w.flush();
        }

        return stringWriter.toString();
    }

    public static String writePublicKey(PublicKey pk) throws Exception {
        StringWriter stringWriter = new StringWriter();

        JcaPEMWriter w = new JcaPEMWriter(stringWriter);
        w.writeObject(pk);
        w.flush();
        IOUtils.closeQuietly(w);

        return stringWriter.toString();
    }

    public static String sshRsaTextFormat(RSAPublicKey key) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, HEADER);
        write(out, key.getPublicExponent().toByteArray());
        write(out, key.getModulus().toByteArray());

        return String.format(SSH_FORMAT.get(), Base64.encodeBase64String(out.toByteArray()));
    }

    protected static void write(OutputStream os, byte[] content) throws IOException {
        byte[] length = new byte[4];
        length[0] = (byte) ((content.length >>> 24) & 0xff);
        length[1] = (byte) ((content.length >>> 16) & 0xff);
        length[2] = (byte) ((content.length >>> 8) & 0xff);
        length[3] = (byte) (content.length & 0xff);

        os.write(length);
        os.write(content);
    }

}
