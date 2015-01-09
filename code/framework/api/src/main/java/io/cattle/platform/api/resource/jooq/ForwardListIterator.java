package io.cattle.platform.api.resource.jooq;

import java.util.Iterator;
import java.util.ListIterator;

public class ForwardListIterator implements ListIterator<Object> {

    Iterator<Object> iter;
    int index;

    public ForwardListIterator(int index, Iterator<Object> iter) {
        this.iter = iter;
        this.index = index;
        for (int i = 0; i < index; i++) {
            iter.next();
        }
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public Object next() {
        index++;
        return iter.next();
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    @Override
    public Object previous() {
        return null;
    }

    @Override
    public int nextIndex() {
        return index + 1;
    }

    @Override
    public int previousIndex() {
        return index - 1;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(Object e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(Object e) {
        throw new UnsupportedOperationException();
    }

}
