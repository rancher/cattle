package io.github.ibuildthecloud.gdapi.doc;

import java.util.Map;

public class TypeDocumentation {
    String id;
    String description;
    Map<String, FieldDocumentation> resourceFields;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, FieldDocumentation> getResourceFields() {
        return resourceFields;
    }

    public void setResourceFields(Map<String, FieldDocumentation> resourceFields) {
        this.resourceFields = resourceFields;
    }

}
