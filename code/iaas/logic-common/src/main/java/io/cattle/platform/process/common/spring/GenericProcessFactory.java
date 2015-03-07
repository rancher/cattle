package io.cattle.platform.process.common.spring;

import io.cattle.platform.engine.process.impl.ResourceStatesDefinition;
import io.cattle.platform.process.common.generic.GenericResourceProcessDefinition;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class GenericProcessFactory {

    String name;
    String start;
    String transitioning;
    String done;
    String stateField;

    public GenericResourceProcessDefinition getObject() throws Exception {
        GenericResourceProcessDefinition definition = new GenericResourceProcessDefinition();
        definition.setName(name);

        ResourceStatesDefinition statesDef = new ResourceStatesDefinition();
        if (stateField != null) {
            statesDef.setStateField(stateField);
        }
        statesDef.setStartStates(getSet(start));
        statesDef.setTransitioningStatesMap(getMap(transitioning));
        statesDef.setDoneStatesMap(getMap(done));

        definition.setStatesDefinition(statesDef);

        return definition;
    }

    protected Set<String> getSet(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptySet();
        }

        return new LinkedHashSet<String>(Arrays.asList(value.trim().split("\\s*,\\s*")));
    }

    protected Map<String, String> getMap(String values) {
        if (StringUtils.isBlank(values)) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new LinkedHashMap<String, String>();
        if (values.indexOf("=") == -1 && values.indexOf(",") == -1) {
            result.put(null, values.trim());
        } else {
            for (String value : values.trim().split("\\s*,\\s*")) {
                if (value.indexOf('=') == -1) {
                    throw new IllegalArgumentException("Setting must be a single state or a list of states in the form from1=to1,from2=to2");
                } else {
                    String[] parts = value.split("=");
                    result.put(parts[0], parts[1]);
                }
            }
        }

        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getTransitioning() {
        return transitioning;
    }

    public void setTransitioning(String transitioning) {
        this.transitioning = transitioning;
    }

    public String getDone() {
        return done;
    }

    public void setDone(String done) {
        this.done = done;
    }

    public String getStateField() {
        return stateField;
    }

    public void setStateField(String stateField) {
        this.stateField = stateField;
    }

}
