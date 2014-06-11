package io.cattle.platform.configitem.model;

import io.cattle.platform.configitem.model.ItemVersion;

public class DefaultItemVersion implements ItemVersion {

    public static final String LATEST = "latest";

    long revision;
    String sourceRevision;
    boolean latest;

    public DefaultItemVersion() {
    }

    public DefaultItemVersion(long revision, String sourceRevision) {
        this.revision = revision;
        this.sourceRevision = sourceRevision;
    }

    @Override
    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    @Override
    public boolean isLatest() {
        return latest;
    }

    public void setLatest(boolean latest) {
        this.latest = latest;
    }

    @Override
    public String getSourceRevision() {
        return sourceRevision;
    }

    public void setSourceRevision(String sourceRevision) {
        this.sourceRevision = sourceRevision;
    }

    public static DefaultItemVersion fromString(String str) {
        DefaultItemVersion result = new DefaultItemVersion();

        if ( str == null )
            return null;

        if (LATEST.equals(str)) {
            result.setLatest(true);
            return result;
        }

        String[] parts = str.split("-");

        if ( parts.length > 2 ) {
            parts = new String[] { parts[parts.length-2], parts[parts.length-1] };
        }

        if ( parts.length != 2 )
            return null;

        try {
            long version = Long.parseLong(parts[0]);
            result.setRevision(version);
            result.setSourceRevision(parts[1]);
            return result;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    @Override
    public String toExternalForm() {
        if ( latest ) {
            return LATEST;
        }
        return String.format("%s-%s", revision, sourceRevision);
    }

    @Override
    public String toString() {
        return toExternalForm();
    }
}
