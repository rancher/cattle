package io.github.ibuildthecloud.dstack.object.impl;

public class ChildReferenceCacheKey {

    Class<?> parent;
    Class<?> child;

    public ChildReferenceCacheKey(Class<?> parent, Class<?> child) {
        super();
        this.parent = parent;
        this.child = child;
    }

    public Class<?> getParent() {
        return parent;
    }

    public Class<?> getChild() {
        return child;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((child == null) ? 0 : child.hashCode());
        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
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
        ChildReferenceCacheKey other = (ChildReferenceCacheKey) obj;
        if (child == null) {
            if (other.child != null)
                return false;
        } else if (!child.equals(other.child))
            return false;
        if (parent == null) {
            if (other.parent != null)
                return false;
        } else if (!parent.equals(other.parent))
            return false;
        return true;
    }

}
