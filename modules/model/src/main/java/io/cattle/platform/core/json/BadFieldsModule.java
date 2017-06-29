package io.cattle.platform.core.json;

import io.cattle.platform.core.model.tables.records.CredentialRecord;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class BadFieldsModule extends SimpleModule {

    private static final long serialVersionUID = 2742278664927845665L;

    @PostConstruct
    public void init() {
        setMixInAnnotation(CredentialRecord.class, BadFields.class);
    }

    public static interface BadFields {
        @JsonIgnore
        java.lang.String getSecretValue();
    }
}
