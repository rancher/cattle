package io.cattle.platform.process.builder;

import io.cattle.platform.engine.manager.impl.ProcessRecordDao;
import io.cattle.platform.engine.process.ProcessHandlerRegistry;
import io.cattle.platform.engine.process.impl.ResourceStatesDefinition;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.process.common.generic.GenericResourceProcessDefinition;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class GenericResourceProcessDefinitionBuilder {

    ObjectManager objectManager;
    JsonMapper jsonMapper;
    ProcessRecordDao processRecordDao;
    ProcessHandlerRegistry processRegistry;

    String name, start, transitioning, stateField, done, resourceType;
    Map<String, String> renames = Collections.emptyMap();

    public GenericResourceProcessDefinitionBuilder(ObjectManager objectManager, JsonMapper jsonMapper, ProcessRecordDao processRecordDao,
                                                   ProcessHandlerRegistry processRegistry) {
        this.objectManager = objectManager;
        this.jsonMapper = jsonMapper;
        this.processRecordDao = processRecordDao;
        this.processRegistry = processRegistry;
    }

    public void build() {
        ResourceStatesDefinition resourceStatesDefinition = new ResourceStatesDefinition();
        if (!StringUtils.isBlank(stateField)) {
            resourceStatesDefinition.setStateField(stateField);
        }

        resourceStatesDefinition.setStartStates(getSet(start, renames));
        resourceStatesDefinition.setTransitioningStatesMap(getMap(transitioning, renames));
        resourceStatesDefinition.setDoneStatesMap(getMap(done, renames));

        GenericResourceProcessDefinition def = new GenericResourceProcessDefinition(name, processRegistry, resourceType, resourceStatesDefinition,
                objectManager, jsonMapper, processRecordDao);

        processRegistry.addProcess(def);
    }

    protected Set<String> getSet(String values, Map<String, String> renames) {
        if (StringUtils.isBlank(values)) {
            return Collections.emptySet();
        }

        Set<String> result = new LinkedHashSet<>();
        for (String value : values.trim().split("\\s*,\\s*")) {
            String translated = renames.get(value);
            result.add(translated == null ? value : translated);
        }

        return result;
    }

    protected Map<String, String> getMap(String values, Map<String, String> renames) {
        if (StringUtils.isBlank(values)) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new LinkedHashMap<>();
        if (values.indexOf("=") == -1 && values.indexOf(",") == -1) {
            result.put(null, values.trim());
        } else {
            for (String value : values.trim().split("\\s*,\\s*")) {
                if (value.indexOf('=') == -1) {
                    throw new IllegalArgumentException("Setting must be a single value or a list of key/values in the form from1=to1,from2=to2");
                } else {
                    String[] parts = value.split("=");
                    result.put(parts[0], parts[1]);
                }
            }
        }

        return translate(result, renames);
    }

    protected Map<String, String> translate(Map<String, String> values, Map<String, String> renames) {
        Map<String, String> result = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = renames.get(entry.getKey());
            String value = renames.get(entry.getValue());

            result.put((key == null ? entry.getKey() : key), (value == null ? entry.getValue() : value));
        }

        return result;
    }

    public GenericResourceProcessDefinitionBuilder name(String name) {
        this.name = name;
        return this;
    }

    public GenericResourceProcessDefinitionBuilder start(String start) {
        this.start = start;
        return this;
    }

    public GenericResourceProcessDefinitionBuilder transitioning(String transitioning) {
        this.transitioning = transitioning;
        return this;
    }

    public GenericResourceProcessDefinitionBuilder done(String done) {
        this.done = done;
        return this;
    }

    public GenericResourceProcessDefinitionBuilder resourceType(String resourceType) {
        this.resourceType = resourceType;
        return this;
    }


}