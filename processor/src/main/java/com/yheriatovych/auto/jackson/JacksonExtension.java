package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.gabrielittner.auto.value.util.AutoValueUtil;
import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.squareup.javapoet.*;
import com.yheriatovych.auto.jackson.model.AutoClass;
import com.yheriatovych.auto.jackson.model.Property;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;

@AutoService(AutoValueExtension.class)
public class JacksonExtension extends AutoValueExtension {
    @Override
    public String generateClass(Context context, String className, String classToExtend, boolean isFinal) {
        TypeSpec.Builder builder = AutoValueUtil.newTypeSpecBuilder(context, className, classToExtend, isFinal);

        String serializerName = "JacksonSerializer";
        String deserializerName = "JacksonDeserializer";

        AutoClass autoClass = AutoClass.parse(context);

        TypeSpec typeSpec = builder
                .addType(emitSerializer(autoClass, serializerName))
                .addType(emitDeserializer(autoClass, context, deserializerName))
                .addType(emitModule(context, serializerName, deserializerName))
                .build();
        return JavaFile.builder(context.packageName(), typeSpec)
                .skipJavaLangImports(true)
                .build()
                .toString();
    }

    private TypeSpec emitModule(Context context, String serializerName, String deserializerName) {
        TypeElement type = context.autoValueClass();
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addStatement("super($S)", type.getSimpleName())
                .addStatement("addSerializer($T.class, new $N())", type, serializerName)
                .addStatement("addDeserializer($T.class, new $N())",type, deserializerName)
                .build();
        return TypeSpec.classBuilder("JacksonModule")
                .superclass(SimpleModule.class)
                .addModifiers(Modifier.STATIC, Modifier.FINAL)
                .addMethod(constructor)
                .build();
    }

    private TypeSpec emitDeserializer(AutoClass autoClass, Context context, String deserializerName) {
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

        for (Property property : autoClass.getProperties()) {
            ExecutableElement executableElement = property.method();
            TypeMirror returnType = executableElement.getReturnType();
            method.addStatement("$T $N = $L", returnType, property.key(), getDefault(returnType));
        }

        //while loop
        method.beginControlFlow("while (p.getCurrentToken() != $T.END_OBJECT) ", JsonToken.class);
        {
            //TODO check if token is field name
            method.addStatement("String fieldName = p.getCurrentName()");
            method.addStatement("p.nextToken()");

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

            method.addStatement("p.nextToken()");
        }
        method.endControlFlow();

        method.addCode("return ");
        method.addCode(AutoValueUtil.newFinalClassConstructorCall(context, autoClass.allKeys()));

        return TypeSpec.classBuilder(deserializerName)
                .superclass(deserializerType)
                .addModifiers(Modifier.STATIC, Modifier.FINAL)
                .addMethod(method.build())
                .build();
    }

    private CodeBlock getGetterForType(TypeMirror returnType) {
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

    private String getDefault(TypeMirror returnType) {
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

    private TypeSpec emitSerializer(AutoClass autoClass, String serializerName) {
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
//            ExecutableElement element = property.
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

    @Override
    public boolean applicable(Context context) {
        for (Element element : context.autoValueClass().getEnclosedElements()) {
            if(element.getKind() == ElementKind.METHOD) {
                ExecutableElement ee = MoreElements.asExecutable(element);
                if(ee.getModifiers().contains(Modifier.STATIC)
                        && MoreTypes.isTypeOf(Module.class, ee.getReturnType())) {
                    return true;
                }
            }
        }
        return false;
    }
}
