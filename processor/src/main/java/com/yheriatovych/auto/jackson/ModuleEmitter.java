package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.squareup.javapoet.*;
import com.yheriatovych.auto.jackson.model.AutoClass;
import com.yheriatovych.auto.jackson.model.Property;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

public class ModuleEmitter {
    static TypeSpec emitModule(AutoClass autoClass, String serializerName, String deserializerName, String moduleName, ProcessingEnvironment env) {
        TypeElement type = autoClass.getTypeElement();
        TypeMirror clazz = env.getTypeUtils().erasure(autoClass.getType());

        ClassName deserializerClass = ClassName.bestGuess(deserializerName);
        FieldSpec deserializerField = FieldSpec.builder(deserializerClass, "deserializer", Modifier.PRIVATE, Modifier.FINAL)
                .build();

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addStatement("super($S)", type.getSimpleName())
                .addStatement("deserializer = new $T()", deserializerClass)
                .addStatement("addSerializer($T.class, new $N())", clazz, serializerName)
                .addStatement("addDeserializer($T.class, deserializer)",clazz)
                .build();
        return TypeSpec.classBuilder(moduleName)
                .superclass(SimpleModule.class)
                .addModifiers(Modifier.STATIC, Modifier.FINAL)
                .addField(deserializerField)
                .addMethod(constructor)
                .addMethods(emitDefaultSetters(autoClass, moduleName))
                .build();
    }

    private static Iterable<MethodSpec> emitDefaultSetters(AutoClass autoClass, String moduleName) {
        List<MethodSpec> specs = new ArrayList<>();
        for (Property property : autoClass.getProperties()) {
            String methodName = Utils.getDefaultSetterName(property);
            String argName = property.name();
            MethodSpec setter = MethodSpec.methodBuilder(methodName)
                    .returns(ClassName.bestGuess(moduleName))
                    .addParameter(Utils.upperType(property.type()), argName)
                    .addStatement("deserializer.$N($N)", methodName, argName)
                    .addStatement("return this")
                    .build();
            specs.add(setter);
        }
        return specs;
    }
}
