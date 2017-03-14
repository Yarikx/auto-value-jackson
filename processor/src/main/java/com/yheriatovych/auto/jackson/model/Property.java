package com.yheriatovych.auto.jackson.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.common.MoreTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

public class Property {
    private final String key;
    private final ExecutableElement executableElement;
    @Nullable
    private final String customJsonName;
    @Nullable
    private final TypeMirror customSerializer;

    public String name() {
        return key;
    }

    public String jsonName() {
        return customJsonName != null ? customJsonName : key;
    }

    Property(String key, ExecutableElement executableElement, @Nullable String customJsonName, TypeMirror customSerializer) {
        this.key = key;
        this.executableElement = executableElement;
        this.customJsonName = customJsonName;
        this.customSerializer = customSerializer;
    }

    public static Property parse(String key, ExecutableElement executableElement, ProcessingEnvironment env) {
        String customName = null;
        JsonProperty jsonProperty = executableElement.getAnnotation(JsonProperty.class);
        if(jsonProperty != null) {
            String value = jsonProperty.value();
            if(!JsonProperty.USE_DEFAULT_NAME.equals(value)) {
                customName = value;
            }
        }

        TypeMirror customSerializer = null;
        JsonSerialize jsonSerialize = executableElement.getAnnotation(JsonSerialize.class);
        if(jsonSerialize != null) {
            try {
                Class<? extends JsonSerializer> serializerClass = jsonSerialize.using();
                customSerializer = env.getElementUtils().getTypeElement(serializerClass.getCanonicalName())
                        .asType();
            } catch (MirroredTypeException mte) {
                customSerializer = mte.getTypeMirror();
            }
        }

        return new Property(key, executableElement, customName, customSerializer);
    }

    public ExecutableElement method() {
        return executableElement;
    }

    public TypeMirror type() {
        return executableElement.getReturnType();
    }

    @Nullable
    public TypeMirror customSerializer() {
        return customSerializer;
    }
}
