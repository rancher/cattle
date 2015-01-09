package io.cattle.platform.db.jooq.generator;

import io.cattle.platform.db.jooq.utils.TableRecordJaxb;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jooq.util.DefaultGeneratorStrategy;
import org.jooq.util.Definition;

public class ModelStrategy extends DefaultGeneratorStrategy {

    @Override
    public String getJavaClassName(Definition definition, Mode mode) {
        if (mode == Mode.INTERFACE) {
            String result = super.getJavaClassName(definition, mode);
            return StringUtils.removeStart(result, "I");
        } else if (mode == Mode.DEFAULT) {
            return super.getJavaClassName(definition, mode) + "Table";
        }
        return super.getJavaClassName(definition, mode);
    }

    @Override
    public String getJavaPackageName(Definition definition, Mode mode) {
        if (mode == Mode.INTERFACE) {
            String result = super.getJavaPackageName(definition, mode);
            return StringUtils.replace(result, ".tables.interfaces", "");
        }
        return super.getJavaPackageName(definition, mode);
    }

    @Override
    public List<String> getJavaClassImplements(Definition definition, Mode mode) {
        List<String> result = super.getJavaClassImplements(definition, mode);
        if (mode == Mode.RECORD) {
            result.add(TableRecordJaxb.class.getName());
        }
        return result;
    }

}
