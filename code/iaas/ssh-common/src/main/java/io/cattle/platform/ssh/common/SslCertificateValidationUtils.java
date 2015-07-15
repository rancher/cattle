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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.openssl.PEMReader;

public class SslCertificateValidationUtils {

    public static void verifySelfSignedCertificate(String certInput, String keyInput) throws Exception {
        X509CertificateObject cert = getCertificateFromPem(certInput);
        PublicKey publicKey = getPublicKey(keyInput);
        cert.verify(publicKey);
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
        if (certChain.size() > 1 && rootCerts.size() != 1) {
            throw new RuntimeException("There should be only one root certificate");
        }
        verifyCertificateChain(cert, rootCerts, intermediateCerts);
    }

    private static X509CertificateObject getCertificateFromPem(String certInput) throws IOException {
        StringReader reader = new StringReader(certInput);
        PEMReader pr = new PEMReader(reader);
        try {
            X509CertificateObject obj = (X509CertificateObject) pr.readObject();
            return obj;
        } finally {
            pr.close();
        }
    }

    private static PublicKey getPublicKey(String keyInput) throws IOException {
        StringReader reader = new StringReader(keyInput);
        PEMReader pr = new PEMReader(reader);
        try {
            KeyPair keyPair = (KeyPair) pr.readObject();
            return keyPair.getPublic();
        } finally {
            pr.close();
        }
    }

    private static List<X509CertificateObject> getCertificateChain(String certChainInput) throws IOException {
        StringReader reader = new StringReader(certChainInput);
        PEMReader pr = new PEMReader(reader);
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
