package io.cattle.platform.engine.handler;

public abstract class AbstractProcessLogic implements ProcessLogic {

    String name = getClass().getSimpleName();

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
