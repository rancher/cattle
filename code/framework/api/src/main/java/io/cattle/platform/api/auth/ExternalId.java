package io.cattle.platform.api.auth;



public class ExternalId {

    private final String id;
    private final String type;
    private final String name;

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }


    public ExternalId(String externalId, String externalIdType) {
        this.id = externalId;
        this.type = externalIdType;
        this.name = null;
    }
    
    public ExternalId(String externalId, String externalIdType, String name) {
        this.id = externalId;
        this.type = externalIdType;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExternalId that = (ExternalId) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        if (type != null ? !type.equals(that.type) : that.type != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
