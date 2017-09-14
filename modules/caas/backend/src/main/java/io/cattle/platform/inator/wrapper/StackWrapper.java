package io.cattle.platform.inator.wrapper;

import io.cattle.platform.core.model.Stack;

public class StackWrapper {

    Stack stack;

    public StackWrapper(Stack stack) {
        super();
        this.stack = stack;
    }

    public String getName() {
        return stack.getName();
    }

    public Long getId() {
        return stack.getId();
    }

    public Long getClusterId() {
        return stack.getClusterId();
    }

    public Long getAccountId() {
        return stack.getAccountId();
    }

}
