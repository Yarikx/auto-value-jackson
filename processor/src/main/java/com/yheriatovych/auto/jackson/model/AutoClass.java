package com.yheriatovych.auto.jackson.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.value.extension.AutoValueExtension;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AutoClass {
    final TypeElement typeElement;
    final List<Property> properties;
    final List<? extends TypeParameterElement> typeParams;
    final @Nullable
    ExecutableElement jsonCreator;

    AutoClass(TypeElement typeElement, List<Property> properties, @Nullable ExecutableElement jsonCreator) {
        this.typeElement = typeElement;
        this.properties = properties;
        this.typeParams = typeElement.getTypeParameters();
        this.jsonCreator = jsonCreator;
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

    public List<? extends TypeParameterElement> getTypeParams() {
        return typeParams;
    }

    public static AutoClass parse(AutoValueExtension.Context context) {
        Types typeUtils = context.processingEnvironment().getTypeUtils();

        List<Property> properties = new ArrayList<>();
        for (String key : context.properties().keySet()) {
            ExecutableElement executableElement = context.properties().get(key);
            Property property = Property.parse(key, executableElement, context.processingEnvironment());
            properties.add(property);
        }

        ExecutableElement jsonCreator = null;
        for (Element element : context.autoValueClass().getEnclosedElements()) {
            //should be method
            if (element.getKind() != ElementKind.METHOD) continue;

            ExecutableElement executableElement = MoreElements.asExecutable(element);
            Set<Modifier> modifiers = executableElement.getModifiers();

            //should be annotated with JsonCreator and be static
            if (executableElement.getAnnotation(JsonCreator.class) == null
                    || !modifiers.contains(Modifier.STATIC)) continue;

            TypeMirror returnType = executableElement.getReturnType();
            //should return itself
            if (returnType.getKind() != TypeKind.DECLARED || !MoreTypes.asDeclared(returnType).asElement().equals(context.autoValueClass()))
                continue;

            //type signature should match auto value properties
            List<? extends VariableElement> parameters = executableElement.getParameters();

            if (parameters.size() != context.properties().size()) continue;

            ArrayList<ExecutableElement> propMethods = new ArrayList<>(context.properties().values());
            boolean match = true;
            for (int i = 0; i < parameters.size(); i++) {
                VariableElement param = parameters.get(i);
                TypeMirror propType = propMethods.get(i).getReturnType();
                if (!typeUtils.isSameType(param.asType(), propType)) {
                    match = false;
                    break;
                }
            }

            if (match) {
                jsonCreator = executableElement;
            }
        }

        return new AutoClass(context.autoValueClass(), properties, jsonCreator);
    }

    public Object[] allKeys() {
        String[] keys = new String[properties.size()];
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            keys[i] = property.name();
        }
        return keys;
    }

    public boolean isGeneric() {
        return !typeParams.isEmpty();
    }

    @Nullable
    public ExecutableElement getJsonCreator() {
        return jsonCreator;
    }
}
