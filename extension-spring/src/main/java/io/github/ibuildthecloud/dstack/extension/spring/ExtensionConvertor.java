package io.github.ibuildthecloud.dstack.extension.spring;

import io.github.ibuildthecloud.dstack.extension.ExtensionManager;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

public class ExtensionConvertor implements GenericConverter {

    public ExtensionConvertor() {
        super();
        this.extensionManager = extensionManager;
    }

    ExtensionManager extensionManager;


    public ExtensionManager getExtensionManager() {
        return extensionManager;
    }

//    @Inject
    public void setExtensionManager(ExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return null;
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        return null;
    }
}
