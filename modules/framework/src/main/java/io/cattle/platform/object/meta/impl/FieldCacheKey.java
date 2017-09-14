package io.cattle.platform.object.meta.impl;

public class FieldCacheKey {

    Class<?> clz;
    String fieldName;

    public FieldCacheKey(Class<?> clz, String fieldName) {
        super();
        this.clz = clz;
        this.fieldName = fieldName;
    }

    public Class<?> getClz() {
        return clz;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clz == null) ? 0 : clz.hashCode());
        result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FieldCacheKey other = (FieldCacheKey) obj;
        if (clz == null) {
            if (other.clz != null)
                return false;
        } else if (!clz.equals(other.clz))
            return false;
        if (fieldName == null) {
            if (other.fieldName != null)
                return false;
        } else if (!fieldName.equals(other.fieldName))
            return false;
        return true;
    }

}
