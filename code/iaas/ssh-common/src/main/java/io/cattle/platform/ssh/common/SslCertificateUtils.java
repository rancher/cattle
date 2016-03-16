package io.cattle.platform.ssh.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.DigestInputStream;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;

public class SslCertificateUtils {

    public static Integer getKeySize(String certInput) throws Exception {
        X509Certificate cert = getCertificateFromPem(certInput);
        PublicKey key = cert.getPublicKey();
        if (key instanceof RSAPublicKey) {
            RSAPublicKey keySpec = (RSAPublicKey) key;
            return keySpec.getModulus().bitLength();
        }
        return null;
    }

    public static List<?> getSubjectAlternativeNames(String certInput) throws Exception {
        X509Certificate cert = getCertificateFromPem(certInput);
        Collection<List<?>> names = cert.getSubjectAlternativeNames();
        if (names == null) {
            return null;
        }

        List<String> altNames = new ArrayList<>();
        Iterator<List<?>> it = names.iterator();
        while (it.hasNext()) {
            List<?> obj = it.next();
            // first value is type identifier (IP, DNS, etc), so adding second only
            altNames.add(obj.get(1).toString());
        }
        return altNames;
    }

    public static String getExpirationDate(String certInput) throws Exception {
        X509Certificate cert = getCertificateFromPem(certInput);
        return cert.getNotAfter().toString();
    }

    public static String getSerialNumber(String certInput) throws Exception {
        X509Certificate cert = getCertificateFromPem(certInput);
        return cert.getSerialNumber().toString();
    }

    public static String getAlgorithm(String certInput) throws Exception {
        X509Certificate cert = getCertificateFromPem(certInput);
        return cert.getSigAlgName();
    }

    public static String getVersion(String certInput) throws Exception {
        X509Certificate cert = getCertificateFromPem(certInput);
        return String.valueOf(cert.getVersion());
    }

    public static String getIssuedDate(String certInput) throws Exception {
        X509Certificate cert = getCertificateFromPem(certInput);
        return cert.getNotBefore().toString();
    }

    public static String getIssuer(String certInput) throws Exception {
        X509Certificate cert = getCertificateFromPem(certInput);
        return cert.getIssuerX500Principal().getName();
    }

    public static String getCN(String certInput) throws Exception {
        X509Certificate cert = getCertificateFromPem(certInput);
        String dn = cert.getSubjectX500Principal().getName();
        LdapName ln = new LdapName(dn);

        for (Rdn rdn : ln.getRdns()) {
            if (rdn.getType().equalsIgnoreCase("CN")) {
                return rdn.getValue().toString();
            }
        }
        return null;
    }

    private static X509Certificate getCertificateFromPem(String certInput) throws IOException, CertificateException {
        StringReader reader = new StringReader(certInput);
        PEMParser pr = new PEMParser(reader);
        try {
            PemObject pem = pr.readPemObject();
            X509CertificateHolder holder = new X509CertificateHolder(pem.getContent());
            return new JcaX509CertificateConverter().setProvider("BC")
                    .getCertificate(holder);
        } finally {
            pr.close();
        }
    }

    /**
     * Obtains the fingerprint of the certificate in the "ab:cd:ef:...:12" format.
     */
    public static String getCertificateFingerprint(String certInput) throws Exception {
        X509Certificate cert = getCertificateFromPem(certInput);
        if (cert == null) {
            return null;
        }
        return digest(cert);
    }

    static String digest(X509Certificate k) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("SHA1");
        DigestInputStream in = new DigestInputStream(new ByteArrayInputStream(k.getEncoded()), md5);
        try {
            while (in.read(new byte[128]) > 0) {
            }
        } finally {
            in.close();
        }
        StringBuilder buf = new StringBuilder();
        char[] hex = Hex.encodeHex(md5.digest());
        for (int i = 0; i < hex.length; i += 2) {
            if (buf.length() > 0)
                buf.append(':');
            buf.append(hex, i, 2);
        }
        return buf.toString();
    }
}
