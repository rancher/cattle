package io.cattle.platform.configitem.server.model.impl;

import io.cattle.platform.configitem.context.ConfigItemContextFactory;
import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.server.model.Request;
import io.cattle.platform.configitem.server.resource.ResourceRoot;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;

public abstract class AbstractArchiveBasedConfigItem extends AbstractResourceRootConfigItem {

    List<ConfigItemContextFactory> contextFactories;

    public AbstractArchiveBasedConfigItem(String name, ConfigItemStatusManager versionManager, ResourceRoot resourceRoot,
            List<ConfigItemContextFactory> contextFactories) {
        super(name, versionManager, resourceRoot);

        this.contextFactories = contextFactories;
    }

    @Override
    public void handleRequest(Request req) throws IOException {
        req.setContentType("application/octet-stream");

        OutputStream os = req.getOutputStream();
        GZIPOutputStream gzos = new GZIPOutputStream(os, 8192);
        TarArchiveOutputStream taos = null;

        try {
            taos = new TarArchiveOutputStream(gzos);
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            ArchiveContext context = new ArchiveContext(req, taos, getVersion(req));

            ItemVersion current = req.getCurrentVersion();
            if (current != null && current.toExternalForm().equals(context.getVersion())) {
                writeUpToDate(context);
            } else {
                for (ConfigItemContextFactory factory : contextFactories) {
                    factory.populateContext(req, this, context);
                }

                writeContent(context);
            }

            writeHashes(context);
        } finally {
            IOUtils.closeQuietly(taos);
        }
    }

    protected static void writeHashes(final ArchiveContext context) throws IOException {
        StringBuilder stringContent = new StringBuilder();
        Map<String, String> hashes = context.getHashes();
        for (Map.Entry<String, String> entry : hashes.entrySet()) {
            stringContent.append(entry.getValue());
            stringContent.append(" *");
            stringContent.append(entry.getKey());
            stringContent.append("\n");
        }

        hashes.clear();

        final byte[] content = stringContent.toString().getBytes("UTF-8");
        withEntry(context, "SHA1SUMS", content.length, new WithEntry() {
            @Override
            public void with(OutputStream os) throws IOException {
                os.write(content);
            }
        });

        Map.Entry<String, String> entry = hashes.entrySet().iterator().next();
        final byte[] sumSum = String.format("%s *%s\n", entry.getValue(), entry.getKey()).getBytes("UTF-8");
        withEntry(context, "SHA1SUMSSUM", sumSum.length, new WithEntry() {
            @Override
            public void with(OutputStream os) throws IOException {
                os.write(sumSum);
            }
        });
    }

    protected void writeContent(final ArchiveContext context) throws IOException {
        writeVersion(context);
    }

    protected static void writeVersion(final ArchiveContext context) throws IOException {
        final byte[] content = (context.getVersion() + "\n").getBytes("UTF-8");
        withEntry(context, "version", content.length, new WithEntry() {
            @Override
            public void with(OutputStream os) throws IOException {
                os.write(content);
            }
        });
    }

    protected static void writeUpToDate(final ArchiveContext context) throws IOException {
        final byte[] content = (context.getVersion() + "\n").getBytes("UTF-8");
        withEntry(context, "uptodate", content.length, new WithEntry() {
            @Override
            public void with(OutputStream os) throws IOException {
                os.write(content);
            }
        });
    }

    protected static void withEntry(ArchiveContext context, String entryName, long size, WithEntry with) throws IOException {
        if (size < 0) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            with.with(baos);
            size = baos.size();
            with = new WithEntry() {
                @Override
                public void with(OutputStream os) throws IOException {
                    os.write(baos.toByteArray());
                }
            };
        }

        withEntry(context, getDefaultEntry(context, entryName, size), with);
    };

    protected static void withEntry(ArchiveContext context, TarArchiveEntry entry, WithEntry with) throws IOException {
        try {
            TarArchiveOutputStream taos = context.getOutputStream();
            DigestOutputStream dos = new DigestOutputStream(taos, MessageDigest.getInstance("SHA1"));

            taos.putArchiveEntry(entry);
            with.with(dos);
            taos.closeArchiveEntry();

            String hash = Hex.encodeHexString(dos.getMessageDigest().digest());
            context.getHashes().put(entry.getName(), hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA1 digest", e);
        }
    };

    protected static TarArchiveEntry getDefaultEntry(ArchiveContext context, String name, long size) {
        StringBuilder entryName = new StringBuilder(context.getRequest().getItemName());
        entryName.append("-").append(context.getVersion());
        if (!name.startsWith(File.separator)) {
            entryName.append(File.separator);
        }
        entryName.append(name);

        TarArchiveEntry entry = new TarArchiveEntry(entryName.toString());
        entry.setUserName("root");
        entry.setGroupName("root");
        entry.setMode(0644);
        entry.setSize(size);
        entry.setModTime(new Date(System.currentTimeMillis() - (60 * 60 * 24 * 1000)));
        return entry;
    }

    @Override
    public String getSourceRevision() {
        String hash = super.getSourceRevision();
        for (ConfigItemContextFactory factory : contextFactories) {
            hash = factory.getContentHash(hash);
        }

        return hash;
    }

    protected interface WithEntry {
        public void with(OutputStream os) throws IOException;
    }
}