package io.cattle.platform.configitem.server.resource;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringListProperty;

public abstract class AbstractCachingResourceRoot implements ResourceRoot {

    public static final DynamicStringListProperty IGNORE_PREFIX = ArchaiusUtil.getList("config.item.ignore.prefixes");
    public static final DynamicBooleanProperty DYNAMIC = ArchaiusUtil.getBoolean("config.item.dynamic");

    Collection<Resource> resources;
    String sourceRevision;
    Callable<byte[]> additionalRevisionData;

    public static boolean shouldIgnore(File base, String path) {
        for (String prefix : IGNORE_PREFIX.get()) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }

        return base == null ? false : Files.isSymbolicLink(new File(base, path).toPath());
    }

    @Override
    public Collection<Resource> getResources() throws IOException {
        return resources;
    }

    protected boolean isDynamic() {
        return DYNAMIC.get();
    }

    @Override
    public synchronized final void scan() throws IOException {
        if (!isDynamic() && sourceRevision != null && resources != null) {
            return;
        }

        Collection<Resource> resources = scanResources();
        DigestOutputStream outputStream = null;
        String revision = null;
        try {
            outputStream = new DigestOutputStream(new NullOutputStream(), MessageDigest.getInstance("SHA-256"));

            if (additionalRevisionData != null) {
                try {
                    outputStream.write(additionalRevisionData.call());
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }

            for (Resource resource : resources) {
                byte[] nameBytes = resource.getName().getBytes();
                outputStream.write(nameBytes, 0, nameBytes.length);

                InputStream is = null;
                try {
                    is = resource.getInputStream();
                    IOUtils.copy(is, outputStream);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }

            outputStream.close();
            revision = Hex.encodeHexString(outputStream.getMessageDigest().digest());
            outputStream = null;
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        this.resources = resources;
        this.sourceRevision = revision;
    }

    protected abstract Collection<Resource> scanResources() throws IOException;

    @Override
    public String getSourceRevision() {
        return sourceRevision;
    }

    public Callable<byte[]> getAdditionalRevisionData() {
        return additionalRevisionData;
    }

    public void setAdditionalRevisionData(Callable<byte[]> additionalRevisionData) {
        this.additionalRevisionData = additionalRevisionData;
    }

}
