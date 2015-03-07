package io.cattle.platform.docker.client;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import com.netflix.config.DynamicStringProperty;

public class DockerImage {

    String id, repository, namespace, tag, server, serverAddress;
    private static final DynamicStringProperty INDEX_URL = ArchaiusUtil.getString("docker.index.url");
    private static final DynamicStringProperty INDEX_SERVER = ArchaiusUtil.getString("docker.index.server");

    public DockerImage(String id, String repository, String namespace, String tag, String server) {
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

    public static DockerImage parse(String id) {
        String namespace = null;
        String repo = null;
        String tag = null;
        String server = null;
        if (id == null) {
            return null;
        }
        String[] forwardSlash = id.split("/");
        switch (forwardSlash.length) {
        case 1:
            String[] split2 = forwardSlash[0].split(":");
            if (split2.length == 1) {
                tag = "latest";
                repo = id;
            } else if (split2.length == 2) {
                tag = split2[1];
                repo = split2[0];
            }
            break;
        case 2:
            String first = forwardSlash[0];
            String[] second = forwardSlash[1].split(":");
            if (first.contains(".") || first.contains(":") || first.equals("localhost")) {
                server = first;
            } else {
                namespace = first;
            }
            if (second.length == 1) {
                repo = forwardSlash[1];
                tag = "latest";
            } else if (second.length == 2) {
                repo = second[0];
                tag = second[1];
            } else {
                return null;
            }
            break;
        case 3:
            server = forwardSlash[0];
            namespace = forwardSlash[1];
            split2 = forwardSlash[2].split(":");
            if (split2.length == 1) {
                repo = forwardSlash[2];
                tag = "latest";
            } else if (split2.length == 2) {
                repo = split2[0];
                tag = split2[1];
            } else {
                return null;
            }
            break;
        default:
            return null;
        }
        return new DockerImage(null, repo, namespace, tag, server);
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
        return getQualifiedName() + ":" + tag;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }
}
