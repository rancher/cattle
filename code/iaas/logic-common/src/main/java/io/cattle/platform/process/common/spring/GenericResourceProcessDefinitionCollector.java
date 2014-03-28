package io.cattle.platform.process.common.spring;

import io.cattle.platform.process.common.generic.GenericResourceProcessDefinition;

import java.util.List;

import javax.inject.Inject;

public class GenericResourceProcessDefinitionCollector {

    List<GenericResourceProcessDefinition> definitions;

    public List<GenericResourceProcessDefinition> getDefinitions() {
        return definitions;
    }

    @Inject
    public void setDefinitions(List<GenericResourceProcessDefinition> definitions) {
        this.definitions = definitions;
    }

}
