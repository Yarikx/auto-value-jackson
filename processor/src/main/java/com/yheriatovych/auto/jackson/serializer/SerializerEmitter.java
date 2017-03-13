package com.yheriatovych.auto.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.yheriatovych.auto.jackson.model.AutoClass;
import com.yheriatovych.auto.jackson.model.Property;

import javax.lang.model.element.Modifier;
import java.io.IOException;

public class SerializerEmitter {
    public static TypeSpec emitSerializer(AutoClass autoClass, String serializerName) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("serializeWithType")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(autoClass.getTypeElement()), "value")
                .addParameter(JsonGenerator.class, "gen")
                .addParameter(SerializerProvider.class, "serializers")
                .addParameter(TypeSerializer.class, "typeSer")
                .addException(IOException.class);

        method.beginControlFlow("if (typeSer != null)")
                .addStatement("typeSer.writeTypePrefixForObject(value, gen, $T.class)", autoClass.getTypeElement())
                .nextControlFlow("else")
                .addStatement("gen.writeStartObject()")
                .endControlFlow();
        for (Property property : autoClass.getProperties()) {
            method.addStatement("gen.writeFieldName($S)", property.jsonName());
            method.addStatement("gen.writeObject(value.$N())", property.key());
        }
        method.beginControlFlow("if (typeSer != null)")
                .addStatement("typeSer.writeTypeSuffixForObject(value, gen)")
                .nextControlFlow("else")
                .addStatement("gen.writeEndObject()")
                .endControlFlow();

        MethodSpec.Builder simpleSerialize = MethodSpec.methodBuilder("serialize")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(autoClass.getTypeElement()), "value")
                .addParameter(JsonGenerator.class, "gen")
                .addParameter(SerializerProvider.class, "serializers")
                .addException(IOException.class)
                .addException(JsonProcessingException.class)
                .addStatement("this.serializeWithType(value, gen, serializers, null)");

        return TypeSpec.classBuilder(serializerName)
                .superclass(ParameterizedTypeName.get(
                        ClassName.get(JsonSerializer.class),
                        ClassName.get(autoClass.getTypeElement())
                ))
                .addModifiers(Modifier.STATIC, Modifier.FINAL)
                .addMethod(method.build())
                .addMethod(simpleSerialize.build())
                .build();
    }
}
