package com.yheriatovych.auto.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.squareup.javapoet.*;
import com.yheriatovych.auto.jackson.model.AutoClass;
import com.yheriatovych.auto.jackson.model.Property;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;

public class SerializerEmitter {
    public static TypeSpec emitSerializer(AutoClass autoClass, String serializerName, ProcessingEnvironment env) {
        SerializerDispatcher serializerDispatcher = new SerializerDispatcher();
        TypeElement typeElement = autoClass.getTypeElement();
        TypeMirror clazz = env.getTypeUtils().erasure(autoClass.getType());
        MethodSpec.Builder method = MethodSpec.methodBuilder("serializeWithType")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(typeElement), "value")
                .addParameter(JsonGenerator.class, "gen")
                .addParameter(SerializerProvider.class, "serializers")
                .addParameter(TypeSerializer.class, "typeSer")
                .addException(IOException.class);

        method.beginControlFlow("if (typeSer != null)")
                .addStatement("typeSer.writeTypePrefixForObject(value, gen, $T.class)", clazz)
                .nextControlFlow("else")
                .addStatement("gen.writeStartObject()")
                .endControlFlow();
        method.addStatement("String fieldName = null")
                .beginControlFlow("try");
        for (Property property : autoClass.getProperties()) {
            method.addStatement("fieldName = $S", property.jsonName())
                    .addStatement("gen.writeFieldName(fieldName)")
                    .addCode(serializerDispatcher.serialize(property))
                    .addCode(";\n");
        }
        method.nextControlFlow("catch (Exception e)")
                .addStatement("throw $T.wrapWithPath(e, value, fieldName)", JsonMappingException.class)
                .endControlFlow();
        method.beginControlFlow("if (typeSer != null)")
                .addStatement("typeSer.writeTypeSuffixForObject(value, gen)")
                .nextControlFlow("else")
                .addStatement("gen.writeEndObject()")
                .endControlFlow();

        MethodSpec.Builder simpleSerialize = MethodSpec.methodBuilder("serialize")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(typeElement), "value")
                .addParameter(JsonGenerator.class, "gen")
                .addParameter(SerializerProvider.class, "serializers")
                .addException(IOException.class)
                .addException(JsonProcessingException.class)
                .addStatement("this.serializeWithType(value, gen, serializers, null)");

        return TypeSpec.classBuilder(serializerName)
                .superclass(ParameterizedTypeName.get(
                        ClassName.get(JsonSerializer.class),
                        ClassName.get(typeElement)
                ))
                .addModifiers(Modifier.STATIC, Modifier.FINAL)
                .addMethod(method.build())
                .addMethod(simpleSerialize.build())
                .build();
    }

}
