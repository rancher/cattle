package io.cattle.platform.process.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static java.util.stream.Collectors.*;

public class TypeProcessBuilder {

    private static final Logger log = LoggerFactory.getLogger(TypeProcessBuilder.class);

    ProcessBuilder builder;
    String type;
    Map<String, String> renames = new HashMap<>();
    Map<String, ProcessTemplate> processes = new TreeMap<>();
    Map<String, ProcessTemplate> templates;

    protected TypeProcessBuilder(ProcessBuilder builder, String type, Map<String, ProcessTemplate> templates) {
        this.builder = builder;
        this.type = type;
        this.templates = templates;
    }

    public ProcessTemplate process(String name) {
        ProcessTemplate template = new ProcessTemplate(builder, this, this.templates.get(name));
        processes.put(name, template);
        return template;
    }

    public ProcessBuilder processes(String... names) {
        if (names.length == 0) {
            throw new IllegalArgumentException("At least one process is required");
        }

        ProcessTemplate template = null;
        for (String name : names) {
            if (template == null) {
                template = process(name);
            } else {
                template = template.process(name);
            }
        }

        return template.build();
    }

    private String processName(String type, String name) {
        return String.format("%s.%s", type, name).toLowerCase();
    }

    public TypeProcessBuilder type(String type) {
        return build().type(type);
    }

    public ProcessBuilder build() {
        Set<String> allStates = new HashSet<>();
        Set<String> restingStates = new HashSet<>();
        Map<String, String> processToTransitioning = new HashMap<>();

        this.processes.forEach((name, template) -> {
            template.validate(processName(type, name));
            allStates.add(template.to);
            allStates.add(template.transitioning);
            restingStates.add(template.to);
            processToTransitioning.put(name, template.transitioning);
        });

        this.processes.forEach((name, template) -> {
            Set<String> startStates = new TreeSet<>(template.from.stream()
                    .filter(state -> {
                        return allStates.contains(state) || builder.blacklist.contains(state);
                    })
                    .collect(toSet()));

            if (template.resting) {
                startStates.addAll(filterBlacklist(restingStates));
            }

            for (String processName : template.during) {
                String state = processToTransitioning.get(processName);
                if (state != null) {
                    startStates.add(state);
                }
            }

            if (startStates.size() == 0) {
                // This means we want to allow starting from everything, except ourself
                startStates.addAll(filterBlacklist(allStates));
                startStates.remove(template.to);
            }

            if (template.self) {
                // Consider self after defaulting to all so we can add back transitioning state if desired
                // (since it's excluded from defaulting to all)
                startStates.add(template.transitioning);
            }

            for (String notAfterProcess : template.notAfter) {
                ProcessTemplate otherProcess = processes.get(notAfterProcess);
                if (otherProcess == null) {
                    continue;
                }

                startStates.remove(otherProcess.transitioning);
                startStates.remove(otherProcess.to);
            }

            for (String notFrom : template.notFrom) {
                startStates.remove(notFrom);
            }

            GenericResourceProcessDefinitionBuilder processDefBuilder =
                    new GenericResourceProcessDefinitionBuilder(builder.objectManager,
                            builder.jsonMapper,
                            builder.processRecordDao,
                            builder.processRegistry);

            String processName = processName(type, name);
            String start = String.join(",", startStates);

            log.info("Registering {}: {} => {} => {}", processName, start, template.transitioning, template.to);

            processDefBuilder
                    .resourceType(type)
                    .name(processName)
                    .start(start)
                    .transitioning(template.transitioning)
                    .done(template.to)
                    .build();
        });

        return builder;
    }

    protected Set<String> filterBlacklist(Collection<String> states) {
        return states.stream().filter(x -> !builder.blacklist.contains(x)).collect(toSet());
    }

}
