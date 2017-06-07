package com.yheriatovych.auto.jackson.model;

import com.google.auto.common.MoreTypes;
import com.google.auto.value.extension.AutoValueExtension;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import java.util.ArrayList;
import java.util.List;

public class AutoClass {
    final TypeElement typeElement;
    final List<Property> properties;
    final List<? extends TypeParameterElement> typeParams;

    AutoClass(TypeElement typeElement, List<Property> properties) {
        this.typeElement = typeElement;
        this.properties = properties;
        this.typeParams = typeElement.getTypeParameters();
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public DeclaredType getType() {
        return MoreTypes.asDeclared(typeElement.asType());
    }

    public List<Property> getProperties() {
        return properties;
    }


    public static AutoClass parse(AutoValueExtension.Context context) {
        List<Property> properties = new ArrayList<>();
        for (String key : context.properties().keySet()) {
            ExecutableElement executableElement = context.properties().get(key);
            Property property = Property.parse(key, executableElement, context.processingEnvironment());
            properties.add(property);
        }

        return new AutoClass(context.autoValueClass(), properties);
    }

    public Object[] allKeys() {
        String[] keys = new String[properties.size()];
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            keys[i] = property.name();
        }
        return keys;
    }
}
