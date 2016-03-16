package io.cattle.platform.ssh.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.openssl.PEMParser;

public class SslCertificateUtils {

    public static Integer getKeySize(String certInput) throws Exception {
        X509CertificateObject cert = getCertificateFromPem(certInput);
        PublicKey key = cert.getPublicKey();
        if (key instanceof RSAPublicKey) {
            RSAPublicKey keySpec = (RSAPublicKey) key;
            return keySpec.getModulus().bitLength();
        }
        return null;
    }

    public static List<?> getSubjectAlternativeNames(String certInput) throws Exception {
        X509CertificateObject cert = getCertificateFromPem(certInput);
        @SuppressWarnings("unchecked")
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
        X509CertificateObject cert = getCertificateFromPem(certInput);
        return cert.getNotAfter().toString();
    }

    public static String getSerialNumber(String certInput) throws Exception {
        X509CertificateObject cert = getCertificateFromPem(certInput);
        return cert.getSerialNumber().toString();
    }

    public static String getAlgorithm(String certInput) throws Exception {
        X509CertificateObject cert = getCertificateFromPem(certInput);
        return cert.getSigAlgName();
    }

    public static String getVersion(String certInput) throws Exception {
        X509CertificateObject cert = getCertificateFromPem(certInput);
        return String.valueOf(cert.getVersion());
    }

    public static String getIssuedDate(String certInput) throws Exception {
        X509CertificateObject cert = getCertificateFromPem(certInput);
        return cert.getNotBefore().toString();
    }

    public static String getIssuer(String certInput) throws Exception {
        X509CertificateObject cert = getCertificateFromPem(certInput);
        return cert.getIssuerX500Principal().getName();
    }

    public static String getCN(String certInput) throws Exception {
        X509CertificateObject cert = getCertificateFromPem(certInput);
        String dn = cert.getSubjectX500Principal().getName();
        LdapName ln = new LdapName(dn);

        for (Rdn rdn : ln.getRdns()) {
            if (rdn.getType().equalsIgnoreCase("CN")) {
                return rdn.getValue().toString();
            }
        }
        return null;
    }

    public static void verifySelfSignedCertificate(String certInput, String keyInput) throws Exception {
        X509CertificateObject cert = getCertificateFromPem(certInput);
        PublicKey publicKey = getPublicKey(keyInput);
        cert.verify(publicKey);
        cert.checkValidity();
    }

    public static void verifyCertificateChain(String certInput, String certChainInput, String keyInput)
            throws Exception {
        X509CertificateObject cert = getCertificateFromPem(certInput);
        List<X509CertificateObject> certChain = getCertificateChain(certChainInput);
        List<X509CertificateObject> rootCerts = new ArrayList<>();
        List<X509CertificateObject> intermediateCerts = new ArrayList<>();
        for (X509CertificateObject certFromChain : certChain) {
            certFromChain.checkValidity();
            if (isSelfSigned(certFromChain)) {
                rootCerts.add(certFromChain);
            } else {
                intermediateCerts.add(certFromChain);
            }
        }
        if (certChain.size() > 0) {
            if (rootCerts.size() > 1) {
                throw new RuntimeException("There should be only one root certificate");
            } else if (rootCerts.size() == 0) {
                throw new RuntimeException("Root certificate is missing in the chain");
            }
        }
        verifyCertificateChain(cert, rootCerts, intermediateCerts);
    }

    private static X509CertificateObject getCertificateFromPem(String certInput) throws IOException {
        StringReader reader = new StringReader(certInput);
        PEMParser pr = new PEMParser(reader);
        try {
            X509CertificateObject obj = (X509CertificateObject) pr.readObject();
            return obj;
        } finally {
            pr.close();
        }
    }

    private static PublicKey getPublicKey(String keyInput) throws IOException {
        StringReader reader = new StringReader(keyInput);
        PEMParser pr = new PEMParser(reader);
        try {
            KeyPair keyPair = (KeyPair) pr.readObject();
            return keyPair.getPublic();
        } finally {
            pr.close();
        }
    }

    private static List<X509CertificateObject> getCertificateChain(String certChainInput) throws IOException {
        StringReader reader = new StringReader(certChainInput);
        PEMParser pr = new PEMParser(reader);
        Object obj;
        List<X509CertificateObject> chain = new ArrayList<>();
        try {
            while ((obj=pr.readObject()) != null) {
                if (obj instanceof X509CertificateObject) {
                    X509CertificateObject cert=(X509CertificateObject)obj;
                  chain.add(cert);
                }
            }
            return chain;
        } finally {
            pr.close();
        }
    }

    public static boolean isSelfSigned(X509CertificateObject cert)
            throws CertificateException, NoSuchAlgorithmException,
            NoSuchProviderException {
        try {
            // Try to verify certificate signature with its own public key
            PublicKey key = cert.getPublicKey();
            cert.verify(key);
            return true;
        } catch (SignatureException sigEx) {
            // Invalid signature --> not self-signed
            return false;
        } catch (InvalidKeyException keyEx) {
            // Invalid key --> not self-signed
            return false;
        }
    }

    private static PKIXCertPathBuilderResult verifyCertificateChain(X509CertificateObject cert,
            List<X509CertificateObject> trustedRootCerts,
            List<X509CertificateObject> intermediateCerts) throws GeneralSecurityException {

        // Create the selector that specifies the starting certificate
        X509CertSelector selector = new X509CertSelector();
        selector.setCertificate(cert);

        // Create the trust anchors (set of root CA certificates)
        Set<TrustAnchor> trustAnchors = new HashSet<>();
        for (X509Certificate trustedRootCert : trustedRootCerts) {
            trustAnchors.add(new TrustAnchor(trustedRootCert, null));
        }

        // Configure the PKIX certificate builder algorithm parameters
        PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustAnchors, selector);

        // Disable CRL checks
        pkixParams.setRevocationEnabled(false);

        // Specify a list of intermediate certificates
        // certificate itself has to be added to the list
        intermediateCerts.add(cert);
        CertStore intermediateCertStore = CertStore.getInstance("Collection",
            new CollectionCertStoreParameters(intermediateCerts), "BC");
        pkixParams.addCertStore(intermediateCertStore);

        // Attempt to build the certification chain and verify it
        CertPathBuilder builder = CertPathBuilder.getInstance("PKIX", "BC");
        return (PKIXCertPathBuilderResult) builder.build(pkixParams);
    }

    /**
     * Obtains the fingerprint of the certificate in the "ab:cd:ef:...:12" format.
     */
    public static String getCertificateFingerprint(String certInput) throws Exception {
        X509CertificateObject cert = getCertificateFromPem(certInput);
        if (cert == null) {
            return null;
        }
        return digest(cert);
    }

    static String digest(X509CertificateObject k) throws Exception {
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
