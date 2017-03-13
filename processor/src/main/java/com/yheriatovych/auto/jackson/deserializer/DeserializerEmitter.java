package com.yheriatovych.auto.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.gabrielittner.auto.value.util.AutoValueUtil;
import com.google.auto.value.extension.AutoValueExtension;
import com.squareup.javapoet.*;
import com.yheriatovych.auto.jackson.Utils;
import com.yheriatovych.auto.jackson.model.AutoClass;
import com.yheriatovych.auto.jackson.model.Property;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeserializerEmitter {
    public static TypeSpec emitDeserializer(AutoClass autoClass, AutoValueExtension.Context context, String deserializerName) {
        ParameterizedTypeName deserializerType = ParameterizedTypeName.get(
                ClassName.get(StdDeserializer.class),
                ClassName.get(autoClass.getTypeElement())
        );

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addStatement("super($T.class)", autoClass.getTypeElement())
                .build();
        MethodSpec.Builder method = MethodSpec.methodBuilder("deserialize")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(JsonParser.class, "p")
                .addParameter(DeserializationContext.class, "ctxt")
                .addException(IOException.class)
                .addException(JsonProcessingException.class)
                .returns(ClassName.get(autoClass.getTypeElement()));

        method.beginControlFlow("if (p.getCurrentToken() == $T.START_OBJECT)", JsonToken.class)
                .addStatement("p.nextToken()")
                .endControlFlow();

        //default vars
        for (Property property : autoClass.getProperties()) {
            ExecutableElement executableElement = property.method();
            TypeMirror returnType = executableElement.getReturnType();
            method.addStatement("$T $N = $L", returnType, property.key(), Utils.getDefaultVarName(property.key()));
        }

        //while loop
        method.beginControlFlow("while (p.getCurrentToken() != $T.END_OBJECT) ", JsonToken.class);
        {
            //TODO check if token is field name
            method.addStatement("String fieldName = p.getCurrentName()");
            method.addStatement("p.nextToken()");

            if (!autoClass.getProperties().isEmpty()) {
                boolean isFirst = true;
                for (Property property : autoClass.getProperties()) {
                    if (isFirst) {
                        method.beginControlFlow("if (fieldName.equals($S))", property.jsonName());
                    } else {
                        method.nextControlFlow("else if (fieldName.equals($S))", property.jsonName());
                    }
                    isFirst = false;

                    method.addCode("$N = ", property.key());
                    method.addCode(getGetterForType(property.type()));
                    method.addCode(";\n");
                }
                method.endControlFlow();
            }

            method.addStatement("p.nextToken()");
        }
        method.endControlFlow();

        method.addCode("return ");
        method.addCode(AutoValueUtil.newFinalClassConstructorCall(context, autoClass.allKeys()));

        return TypeSpec.classBuilder(deserializerName)
                .superclass(deserializerType)
                .addModifiers(Modifier.STATIC, Modifier.FINAL)
                .addMethod(constructor)
                .addFields(emitDefaultFields(autoClass))
                .addMethods(emitDefaultSetters(autoClass, deserializerName))
                .addMethod(method.build())
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

    private static Iterable<FieldSpec> emitDefaultFields(AutoClass autoClass) {
        List<FieldSpec> fields = new ArrayList<>();
        for (Property property : autoClass.getProperties()) {
            FieldSpec field = FieldSpec.builder(TypeName.get(property.type()), Utils.getDefaultVarName(property.key()), Modifier.PRIVATE)
                    .build();
            fields.add(field);
        }
        return fields;
    }

    private static CodeBlock getGetterForType(TypeMirror type) {
        return new DeserializerDispatcher()
                .deser(type);
    }

}
