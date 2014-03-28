package io.cattle.platform.process.common.spring;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class DefaultProcessesParser extends ProcessParser {

    private static final String PROP_PREFIX = "generic.resource.process.";

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        String id = element.getAttribute("id");
        String stateField = element.getAttribute("stateField");
        String resourceType = element.getAttribute("resourceType");
        String exclude = element.getAttribute("exclude");
        Map<String,String> renames = getMap(element.getAttribute("renames"), new HashMap<String, String>());
        Map<String,String> processRenames = getMap(element.getAttribute("processRenames"), new HashMap<String, String>());

        Set<String> excludes = new HashSet<String>();
        if ( ! StringUtils.isBlank(exclude) ) {
            excludes.addAll(Arrays.asList(exclude.trim().split("\\s*,\\s*")));
        }

        BeanDefinition last = null;
        for ( int i = 0 ; true ; i++ ) {
            String value = ArchaiusUtil.getString(PROP_PREFIX + i).get();
            if ( StringUtils.isBlank(value) ) {
                break;
            }

            String[] parts = value.trim().split(";");
            if ( parts.length != 4 && parts.length != 5 ) {
                throw new IllegalStateException(PROP_PREFIX + i + " must be in the format name;start;transitioning;done;delegate(optional)");
            }

            String name = String.format(parts[0], resourceType).toLowerCase();
            if ( processRenames.containsKey(name) ) {
                name = processRenames.get(name);
            }
            String start = parts[1];
            String transitioning = parts[2];
            String done = parts[3];
            String delegate = null;
            if ( parts.length > 4 ) {
                delegate = String.format(parts[4], resourceType).toLowerCase();
                if ( processRenames.containsKey(delegate) ) {
                    delegate = processRenames.get(delegate);
                }
            }

            if ( ! excludes.contains(name) ) {
                last = parse(id, stateField, name, start, transitioning, done, resourceType, delegate, renames, parserContext);
            }
        }

        return last;
    }

}
