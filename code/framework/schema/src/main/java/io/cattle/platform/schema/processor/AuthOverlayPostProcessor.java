package io.cattle.platform.schema.processor;

import io.cattle.platform.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.model.Action;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.FieldImpl;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthOverlayPostProcessor implements SchemaPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(AuthOverlayPostProcessor.class);

    Map<String, Perm> perms = new LinkedHashMap<>();
    List<Pair<Pattern, Perm>> wildcards = new ArrayList<>();

    List<URL> resources;
    JsonMapper jsonMapper;

    public AuthOverlayPostProcessor() {
    }

    public AuthOverlayPostProcessor(List<URL> resources, JsonMapper jsonMapper) throws IOException {
        super();
        this.resources = resources;
        this.jsonMapper = jsonMapper;
        init();
    }

    @Override
    public SchemaImpl postProcessRegister(SchemaImpl schema, SchemaFactory factory) {
        Perm perm = getPerm(schema.getId());

        return perm == null || perm.isRead() ? schema : null;
    }

    @Override
    public SchemaImpl postProcess(SchemaImpl schema, SchemaFactory factory) {
        Perm perm = getPerm(schema.getId());

        if (perm != null) {
            schema.setCreate(perm.isCreate());
            schema.setUpdate(perm.isUpdate());
            schema.setDeletable(perm.isDelete());
        }

        Iterator<Map.Entry<String, Field>> fieldIter = schema.getResourceFields().entrySet().iterator();

        while (fieldIter.hasNext()) {
            Map.Entry<String, Field> entry = fieldIter.next();
            Field field = entry.getValue();

            perm = getPerm(factory, schema, entry.getKey());

            if (perm == null) {
                continue;
            }

            if (!perm.isRead()) {
                fieldIter.remove();
            }

            if (!(field instanceof FieldImpl)) {
                continue;
            }

            FieldImpl fieldImpl = (FieldImpl) field;
            fieldImpl.setCreate(perm.isCreate());
            fieldImpl.setUpdate(perm.isUpdate());
            fieldImpl.setReadOnCreateOnly(perm.isReadOnCreateOnly());
        }
        Iterator<Map.Entry<String, Action>> actionIter = schema.getResourceActions().entrySet().iterator();

        while (actionIter.hasNext()) {
            Map.Entry<String, Action> entry = actionIter.next();
            perm = getPerm(factory, schema, "resourceActions." + entry.getKey());
            if (perm == null) {
                continue;
            }
            if (!perm.isCreate()) {
                actionIter.remove();
            }
        }

        return schema;
    }

    protected Perm getPerm(SchemaFactory factory, Schema schema, String field) {
        Schema start = schema;

        while (schema != null) {
            String name = String.format("%s.%s", schema.getId(), field);
            Perm perm = getPerm(name, false);

            if (perm != null)
                return perm;

            schema = factory.getSchema(schema.getParent());
        }

        schema = start;
        while (schema != null) {
            String name = String.format("%s.%s", schema.getId(), field);
            Perm perm = getPerm(name, true);

            if (perm != null)
                return perm;

            schema = factory.getSchema(schema.getParent());
        }

        return null;
    }

    protected Perm getPerm(String name) {
        return getPerm(name, true);
    }

    protected Perm getPerm(String name, boolean wildcard) {
        List<Perm> result = new ArrayList<>();

        if (wildcard) {
            for (Pair<Pattern, Perm> entry : wildcards) {
                if (entry.getLeft().matcher(name).matches()) {
                    result.add(entry.getValue());
                }
            }
        }

        Perm perm = perms.get(name);
        if (perm != null) {
            result.add(perm);
        }

        return result.size() == 0 ? null : result.get(result.size() - 1);
    }

    @PostConstruct
    public void init() throws IOException {
        for (URL url : resources) {
            log.info("Loading [{}] for schema auth", url);

            InputStream is = url.openStream();
            try {
                Map<String, Object> values = jsonMapper.readValue(is);
                Object value = values.get("authorize");
                if (value instanceof Map) {
                    load((Map<?, ?>) value);
                }
            } finally {
                IOUtils.closeQuietly(is);
            }

        }
    }

    protected void load(Map<?, ?> values) {
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            String key = entry.getKey().toString();
            Perm perm = new Perm(entry.getValue().toString());

            if (key.contains("*")) {
                wildcards.add(new ImmutablePair<>(Pattern.compile(key), perm));
            } else {
                perms.put(key, perm);
            }
        }
    }

    public List<URL> getResources() {
        return resources;
    }

    @Inject
    public void setResources(List<URL> resources) {
        this.resources = resources;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    private static class Perm {
        boolean read, create, update, delete, readOnCreateOnly;

        public Perm(String value) {
            super();
            create = value.contains("c");
            read = value.contains("r");
            update = value.contains("u");
            delete = value.contains("d");
            readOnCreateOnly = value.contains("o");
        }

        public boolean isRead() {
            return read;
        }

        public boolean isCreate() {
            return create;
        }

        public boolean isUpdate() {
            return update;
        }

        public boolean isDelete() {
            return delete;
        }

        public boolean isReadOnCreateOnly() {
            return readOnCreateOnly;
        }
    }

}
