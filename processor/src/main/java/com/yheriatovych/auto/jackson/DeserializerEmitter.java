package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.gabrielittner.auto.value.util.AutoValueUtil;
import com.google.auto.common.MoreTypes;
import com.google.auto.value.extension.AutoValueExtension;
import com.squareup.javapoet.*;
import com.yheriatovych.auto.jackson.model.AutoClass;
import com.yheriatovych.auto.jackson.model.Property;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeserializerEmitter {
    static TypeSpec emitDeserializer(AutoClass autoClass, AutoValueExtension.Context context, String deserializerName) {
        ParameterizedTypeName deserializerType = ParameterizedTypeName.get(
                ClassName.get(JsonDeserializer.class),
                ClassName.get(autoClass.getTypeElement())
        );
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
            method.addStatement("$T $N = $L", returnType, property.key(), property.key() + "Default");
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
                .addFields(emitDefaultFields(autoClass))
                .addMethods(emitDefaultSetters(autoClass, deserializerName))
                .addMethod(method.build())
                .build();
    }

    private static Iterable<MethodSpec> emitDefaultSetters(AutoClass autoClass, String deserializerName) {
        List<MethodSpec> specs = new ArrayList<>();
        for (Property property : autoClass.getProperties()) {
            MethodSpec setter = MethodSpec.methodBuilder("set" + capitalizeFirst(property.key()) + "Default")
                    .returns(ClassName.bestGuess(deserializerName))
                    .addParameter(TypeName.get(property.type()), property.key())
                    .addStatement("this.$N = $N", property.key() + "Default", property.key())
                    .addStatement("return this")
                    .build();
            specs.add(setter);
        }
        return specs;
    }

    private static Iterable<FieldSpec> emitDefaultFields(AutoClass autoClass) {
        List<FieldSpec> fields = new ArrayList<>();
        for (Property property : autoClass.getProperties()) {
            FieldSpec field = FieldSpec.builder(TypeName.get(property.type()), property.key() + "Default", Modifier.PRIVATE)
                    .build();
            fields.add(field);
        }
        return fields;
    }

    private static CodeBlock getGetterForType(TypeMirror returnType) {
        switch (returnType.getKind()) {
            default:
                if(returnType.getKind() == TypeKind.DECLARED) {
                    DeclaredType declaredType = MoreTypes.asDeclared(returnType);
                    if(declaredType.getTypeArguments().size() == 0) {
                        return CodeBlock.of("p.readValueAs($T.class)", returnType);
                    } else {
                        return CodeBlock.of("p.readValueAs(new $T<$T>(){})", TypeReference.class, returnType);
                    }
                }
                return CodeBlock.of("p.readValueAs($T.class)", returnType);
        }
    }

    private static String getDefault(TypeMirror returnType) {
        switch (returnType.getKind()) {

            case BOOLEAN:
                return "false";
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
                return "0";
            case CHAR:
                return "'\0'";
            default:
                return "null";
        }
    }

    private static String capitalizeFirst(String str) {
        //TODO properly handle unicode code points
        char first = Character.toUpperCase(str.charAt(0));
        return first + str.substring(1);
    }
}
