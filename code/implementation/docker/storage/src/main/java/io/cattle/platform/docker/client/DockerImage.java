package io.cattle.platform.docker.client;

public class DockerImage {

    String id, repository, namespace, tag;

    public DockerImage(String id, String repository, String namespace, String tag) {
        super();
        this.id = id;
        this.repository = repository;
        this.namespace = namespace;
        this.tag = tag;
    }

    public static DockerImage parse(String id) {
        String namespace = null;
        String repo = null;
        String tag = null;

        if (id == null) {
            return null;
        }

        int i = id.indexOf(":");
        if (i == -1) {
            tag = "latest";
        } else {
            tag = id.substring(i + 1);
            id = id.substring(0, i);
        }

        i = id.indexOf("/");
        if (i == -1) {
            repo = id;
        } else {
            namespace = id.substring(0, i);
            repo = id.substring(i + 1);
        }

        return new DockerImage(null, repo, namespace, tag);
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
}
