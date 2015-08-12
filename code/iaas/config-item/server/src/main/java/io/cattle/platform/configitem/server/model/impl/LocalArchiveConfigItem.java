package io.cattle.platform.configitem.server.model.impl;

import io.cattle.platform.configitem.server.model.Request;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

public class LocalArchiveConfigItem extends AbstractConfigItem {

    File file;
    String revision;

    public LocalArchiveConfigItem(File file, String name, ConfigItemStatusManager versionManager) throws IOException {
        super(name, versionManager);

        this.file = file;
        this.hash();
    }

    public void hash() throws IOException {
        DigestOutputStream os = null;
        try {
            os = new DigestOutputStream(new NullOutputStream(), MessageDigest.getInstance("SHA-256"));
            copyFile(os);
            os.close();
            revision = Hex.encodeHexString(os.getMessageDigest().digest());
            os = null;
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public void handleRequest(Request req) throws IOException {
        req.setContentType("application/octet-stream");
        OutputStream os = req.getOutputStream();
        os.write(String.format("version:%s\n", getVersion(req)).getBytes("UTF-8"));
        copyFile(os);
    }

    protected void copyFile(OutputStream os) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            IOUtils.copyLarge(fis, os, new byte[1024 * 128]);
        }
    }

    @Override
    public String getSourceRevision() {
        return revision;
    }
}
