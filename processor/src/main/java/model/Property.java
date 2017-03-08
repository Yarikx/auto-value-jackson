package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

public class Property {
    private final String key;
    private final ExecutableElement executableElement;
    @Nullable
    private final String customJsonName;

    public String key() {
        return key;
    }

    public String jsonName() {
        return customJsonName != null ? customJsonName : key;
    }

    Property(String key, ExecutableElement executableElement, @Nullable String customJsonName) {
        this.key = key;
        this.executableElement = executableElement;
        this.customJsonName = customJsonName;
    }

    public static Property parse(String key, ExecutableElement executableElement) {
        String customName = null;
        JsonProperty jsonProperty = executableElement.getAnnotation(JsonProperty.class);
        if(jsonProperty != null) {
            String value = jsonProperty.value();
            if(!JsonProperty.USE_DEFAULT_NAME.equals(value)) {
                customName = value;
            }
        }

        return new Property(key, executableElement, customName);
    }

    public ExecutableElement method() {
        return executableElement;
    }

    public TypeMirror type() {
        return executableElement.getReturnType();
    }
}
