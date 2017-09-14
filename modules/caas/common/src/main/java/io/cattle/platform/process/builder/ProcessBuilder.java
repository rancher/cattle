package io.cattle.platform.process.builder;

import io.cattle.platform.engine.manager.impl.ProcessRecordDao;
import io.cattle.platform.engine.process.ProcessHandlerRegistry;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProcessBuilder {

    ObjectManager objectManager;
    JsonMapper jsonMapper;
    ProcessRecordDao processRecordDao;
    ProcessHandlerRegistry processRegistry;

    Set<String> blacklist = new HashSet<>();
    Map<String, ProcessTemplate> templates = new HashMap<>();

    public ProcessBuilder(ObjectManager objectManager, JsonMapper jsonMapper, ProcessRecordDao processRecordDao, ProcessHandlerRegistry processRegistry) {
        this.objectManager = objectManager;
        this.jsonMapper = jsonMapper;
        this.processRecordDao = processRecordDao;
        this.processRegistry = processRegistry;
    }

    public TypeProcessBuilder type(String type) {
        TypeProcessBuilder builder = new TypeProcessBuilder(this, type, templates);
        builder
            .process("create")
            .process("remove");
        return builder;
    }

    public ProcessTemplate template(String name) {
        ProcessTemplate template = new ProcessTemplate(this);
        templates.put(name, template);
        return template;
    }

    public ProcessBuilder blacklist(String... states) {
        Collections.addAll(blacklist, states);
        return this;
    }

}
