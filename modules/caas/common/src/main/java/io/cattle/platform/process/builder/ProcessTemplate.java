package io.cattle.platform.process.builder;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessTemplate {
    ProcessBuilder builder;
    TypeProcessBuilder typeProcessBuilder;

    List<String> from = new ArrayList<>();
    List<String> during = new ArrayList<>();
    List<String> notAfter = new ArrayList<>();
    List<String> notFrom = new ArrayList<>();
    Map<String, String> conditionalTo = new HashMap<>();
    String transitioning;
    String to;
    boolean resting, self;

    protected ProcessTemplate(ProcessBuilder builder) {
        this.builder = builder;
    }

    protected ProcessTemplate(ProcessBuilder builder, TypeProcessBuilder typeProcessBuilder, ProcessTemplate other) {
        this.builder = builder;
        this.typeProcessBuilder = typeProcessBuilder;
        if (other != null) {
            this.from = new ArrayList<>(other.from);
            this.during = new ArrayList<>(other.during);
            this.notAfter = new ArrayList<>(other.notAfter);
            this.notFrom = new ArrayList<>(other.notFrom);
            this.conditionalTo = other.conditionalTo;
            this.transitioning = other.transitioning;
            this.to = other.to;
            this.resting = other.resting;
            this.self = other.self;
        }
    }

    public ProcessTemplate reset() {
        from.clear();
        during.clear();
        notAfter.clear();
        notFrom.clear();
        conditionalTo.clear();
        transitioning = to = null;
        resting = self = false;
        return this;
    }

    public ProcessTemplate from(String... froms) {
        Collections.addAll(this.from, froms);
        return this;
    }

    public ProcessTemplate notAfter(String... froms) {
        Collections.addAll(this.notAfter, froms);
        return this;
    }

    public ProcessTemplate notFrom(String... froms) {
        Collections.addAll(this.notFrom, froms);
        return this;
    }

    public ProcessTemplate transitioning(String state) {
        transitioning = state;
        return this;
    }

    public ProcessTemplate fromSelf() {
        self = true;
        return this;
    }

    public ProcessTemplate fromResting() {
        resting = true;
        return this;
    }

    public ProcessTemplate during(String... processes) {
        Collections.addAll(during, processes);
        return this;
    }

    public ProcessTemplate to(String to) {
        this.to = to;
        return this;
    }

    public TypeProcessBuilder type(String type) {
        return build().type(type);
    }

    public ProcessBuilder build() {
        if (typeProcessBuilder == null) {
            throw new IllegalStateException("Failed to find process builder");
        }
        typeProcessBuilder.build();
        return builder;
    }

    public ProcessTemplate process(String name) {
        if (typeProcessBuilder == null) {
            throw new IllegalStateException("Failed to find process builder");
        }
        return typeProcessBuilder.process(name);
    }

    public ProcessTemplate template(String name) {
        return builder.template(name);
    }

    public void validate(String name) {
        conditionalTo.forEach((process, to) -> {
            if (typeProcessBuilder.processes.containsKey(process)) {
                this.to = to;
            }
        });

        if (StringUtils.isBlank(to)) {
            throw new IllegalStateException("To state is blank for " + name);
        }
        if (StringUtils.isBlank(transitioning)) {
            throw new IllegalStateException("Transitioning state is blank for " + name);
        }
    }

    public ProcessTemplate ifProcessExistThenTo(String process, String to) {
        this.conditionalTo.put(process, to);
        return this;
    }
}
