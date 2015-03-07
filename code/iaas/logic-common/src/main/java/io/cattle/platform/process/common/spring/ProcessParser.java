package io.cattle.platform.process.common.spring;

import io.cattle.platform.engine.process.impl.ResourceStatesDefinition;
import io.cattle.platform.process.common.generic.GenericResourceProcessDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class ProcessParser implements BeanDefinitionParser {

    private static AtomicInteger COUNTER = new AtomicInteger();

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        String id = element.getAttribute("id");
        String name = element.getAttribute("name");
        String start = element.getAttribute("start");
        String transitioning = element.getAttribute("transitioning");
        String stateField = element.getAttribute("stateField");
        String done = element.getAttribute("done");
        String delegate = element.getAttribute("delegate");
        String resourceType = element.getAttribute("resourceType");

        if (StringUtils.isBlank(delegate)) {
            delegate = null;
        }

        return parse(id, stateField, name, start, transitioning, done, resourceType, delegate, new HashMap<String, String>(), parserContext);
    }

    protected BeanDefinition parse(String id, String stateField, String name, String start, String transitioning, String done, String resourceType,
            String delegate, Map<String, String> renames, ParserContext parserContext) {
        if (StringUtils.isBlank(id)) {
            if (StringUtils.isBlank(name)) {
                id = "generated$" + GenericResourceProcessDefinition.class.getSimpleName() + "$" + COUNTER.incrementAndGet();
            } else {
                id = "generated$" + name;
            }
        }

        BeanDefinitionBuilder statesBuilder = BeanDefinitionBuilder.genericBeanDefinition(ResourceStatesDefinition.class);

        if (!StringUtils.isBlank(stateField)) {
            statesBuilder.addPropertyValue("stateField", stateField);
        }

        statesBuilder.addPropertyValue("startStates", getSet(start, renames));
        statesBuilder.addPropertyValue("transitioningStatesMap", getMap(transitioning, renames));
        statesBuilder.addPropertyValue("doneStatesMap", getMap(done, renames));

        String childId = parserContext.getReaderContext().generateBeanName(statesBuilder.getBeanDefinition());

        BeanDefinitionBuilder processDefBuilder = BeanDefinitionBuilder.genericBeanDefinition(GenericResourceProcessDefinition.class);
        processDefBuilder.addPropertyValue("name", name);
        processDefBuilder.addPropertyValue("resourceType", resourceType);
        processDefBuilder.addPropertyValue("processDelegateName", delegate);
        processDefBuilder.addPropertyReference("statesDefinition", childId);

        register(childId, statesBuilder, parserContext);
        return register(id, processDefBuilder, parserContext);
    }

    protected BeanDefinition register(String id, BeanDefinitionBuilder builder, ParserContext parserContext) {
        BeanDefinition def = builder.getBeanDefinition();
        BeanDefinitionHolder holder = new BeanDefinitionHolder(def, id);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());

        return def;
    }

    protected Set<String> getSet(String values, Map<String, String> renames) {
        if (StringUtils.isBlank(values)) {
            return Collections.emptySet();
        }

        Set<String> result = new LinkedHashSet<String>();
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

        Map<String, String> result = new LinkedHashMap<String, String>();
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
        Map<String, String> result = new LinkedHashMap<String, String>();

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = renames.get(entry.getKey());
            String value = renames.get(entry.getValue());

            result.put((key == null ? entry.getKey() : key), (value == null ? entry.getValue() : value));
        }

        return result;
    }

}