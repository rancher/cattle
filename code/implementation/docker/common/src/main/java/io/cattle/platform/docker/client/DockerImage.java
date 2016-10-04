package io.cattle.platform.docker.client;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicStringProperty;

public class DockerImage {

    String id, repository, namespace, tag, server, serverAddress;
    private static final DynamicStringProperty INDEX_URL = ArchaiusUtil.getString("docker.index.url");
    private static final DynamicStringProperty INDEX_SERVER = ArchaiusUtil.getString("docker.index.server");
    public static final String KIND_PREFIX = "docker:";

    public DockerImage(String id, String server, String namespace, String repository, String tag) {
        super();
        this.id = id;
        this.repository = repository;
        this.namespace = namespace;
        this.tag = tag;
        if (server != null) {
            if (server.startsWith("https://")) {
                this.serverAddress = server;
                this.server = server.replaceFirst("https://", "");
            } else {
                this.serverAddress = "https://" + server;
                this.server = server;
            }
        } else {
            this.serverAddress = INDEX_URL.get();
            this.server = INDEX_SERVER.get();
        }
    }

    public static DockerImage parse(String uuid) {
        uuid = StringUtils.removeStart(uuid, KIND_PREFIX);
        if (uuid == null) {
            return null;
        }
        String[] fields;
        String[] forwardSlash = uuid.split("/");
        switch (forwardSlash.length) {
        case 1:
            fields = repoAndTag(uuid);
            break;
        case 2:
            String first = forwardSlash[0];
            String second = forwardSlash[1];
            fields = repoAndTag(second);
            if (first.contains(".") || first.contains(":") || first.equals("localhost")) {
                fields[0] = first;
            } else {
                fields[1] = first;
            }
            break;
        case 3:
            fields = repoAndTag(forwardSlash[2]);
            fields[0] = forwardSlash[0];
            fields[1] = forwardSlash[1];
            break;
        default:
            return null;
        }
        if (fields[2] == null){
            return null;
        }
        return fromArray(fields);
    }

    private static String[] repoAndTag(String uuid){
        String[] fields = new String[4];
        String[] split;
        if (uuid.contains("@")){
            split = uuid.split("@");
            if (split.length == 2 && split[1].startsWith("sha256:")) {
                fields[2] = split[0];
                fields[3] = split[1];
            }
        } else {
            split = uuid.split(":");
            if (split.length == 1) {
                fields[2] = uuid;
                fields[3] = "latest";
            } else if (split.length == 2) {
                fields[2] = split[0];
                fields[3] = split[1];
            }
        }
        return fields;
    }

    private static DockerImage fromArray(String[] fields){
        if (fields.length != 4){
            return null;
        }
        return new DockerImage(null, fields[0], fields[1], fields[2], fields[3]);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getQualifiedName() {
        if (server.equals(INDEX_SERVER.get())) {
            if (namespace == null) {
                return repository;
            } else {
                return namespace + "/" + repository;
            }
        } else {
            if (namespace == null) {
                return server + "/" + repository;
            } else {
                return server + "/" + namespace + "/" + repository;
            }
        }
    }

    public String getLookUpName() {
        if (namespace == null) {
            return repository;
        } else {
            return namespace + "/" + repository;
        }
    }

    @Override
    public String toString() {
        return getFullName();
    }

    public String getFullName() {
        if (tag.startsWith("sha256")){
            return getQualifiedName() + '@' + tag;
        }
        return getQualifiedName() + ":" + tag;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getServerAddress() {
        return serverAddress;
    }
}
