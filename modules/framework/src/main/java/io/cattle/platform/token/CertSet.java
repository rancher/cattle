package io.cattle.platform.token;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

public class CertSet {
    X509Certificate cert;
    X509Certificate ca;
    PrivateKey key;

    public CertSet(X509Certificate ca, X509Certificate cert, PrivateKey key) {
        super();
        this.cert = cert;
        this.ca = ca;
        this.key = key;
    }

    public X509Certificate getCert() {
        return cert;
    }

    public X509Certificate getCa() {
        return ca;
    }

    public PrivateKey getKey() {
        return key;
    }

    public void writeZip(OutputStream output) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(output);
        write(zos, "ca.pem", ca);
        write(zos, "cert.pem", cert);
        write(zos, "key.pem", key);
        zos.close();
    }

    protected void write(ZipOutputStream zos, String name, Object obj) throws IOException {
        ZipEntry ze = new ZipEntry(name);
        zos.putNextEntry(ze);
        @SuppressWarnings("resource")
        JcaPEMWriter writer = new JcaPEMWriter(new OutputStreamWriter(zos));
        writer.writeObject(obj);
        writer.flush();
        zos.closeEntry();
    }

}