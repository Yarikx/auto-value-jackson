package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.yheriatovych.auto.jackson.model.AutoClass;
import com.yheriatovych.auto.jackson.model.Property;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

public class ModuleEmitter {
    static TypeSpec emitModule(AutoClass autoClass, String serializerName, String deserializerName, String moduleName) {
        TypeElement type = autoClass.getTypeElement();
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addStatement("super($S)", type.getSimpleName())
                .addStatement("addSerializer($T.class, new $N())", type, serializerName)
                .addStatement("addDeserializer($T.class, new $N())",type, deserializerName)
                .build();
        return TypeSpec.classBuilder(moduleName)
                .superclass(SimpleModule.class)
                .addModifiers(Modifier.STATIC, Modifier.FINAL)
                .addMethod(constructor)
//                .addMethods(emitDefaultSetters(autoClass, deserializerName))
                .build();
    }

    private static Iterable<MethodSpec> emitDefaultSetters(AutoClass autoClass, String deserializerName) {
        List<MethodSpec> specs = new ArrayList<>();
        for (Property property : autoClass.getProperties()) {
            MethodSpec setter = MethodSpec.methodBuilder(Utils.getDefaultSetterName(property))
                    .returns(ClassName.bestGuess(deserializerName))
                    .addParameter(TypeName.get(property.type()), property.key())
                    .addStatement("this.$N = $N", Utils.getDefaultVarName(property.key()), property.key())
                    .addStatement("return this")
                    .build();
            specs.add(setter);
        }
        return specs;
    }
}
