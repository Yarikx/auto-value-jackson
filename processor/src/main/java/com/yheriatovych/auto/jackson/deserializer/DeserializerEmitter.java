package com.yheriatovych.auto.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.gabrielittner.auto.value.util.AutoValueUtil;
import com.google.auto.common.MoreTypes;
import com.google.auto.value.extension.AutoValueExtension;
import com.squareup.javapoet.*;
import com.yheriatovych.auto.jackson.Utils;
import com.yheriatovych.auto.jackson.model.AutoClass;
import com.yheriatovych.auto.jackson.model.Property;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.*;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeserializerEmitter {

    public static TypeSpec emitDeserializer(AutoClass autoClass, AutoValueExtension.Context context, String deserializerName) {
        ProcessingEnvironment env = context.processingEnvironment();
        TypeMirror clazz = env.getTypeUtils().erasure(autoClass.getType());

        DeserializerDispatcher deserializerDispatcher = new DeserializerDispatcher(autoClass);
        ParameterizedTypeName deserializerType = ParameterizedTypeName.get(
                ClassName.get(StdDeserializer.class),
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
            method.addStatement("$T $N = $L", Utils.upperType(returnType), property.name(), Utils.getDefaultVarName(property.name()));
        }

        //while loop
        method.beginControlFlow("while (p.getCurrentToken() != $T.END_OBJECT) ", JsonToken.class);
        {
            //TODO check if token is field name
            method.addStatement("String fieldName = p.getCurrentName()");
            method.beginControlFlow("try");
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

                    method.addCode("$N = ", property.name());
                    method.addCode(deserializerDispatcher.deser(property));
                    method.addCode(";\n");
                }
                method.nextControlFlow("else")
                        .addStatement("p.skipChildren()")
                        .endControlFlow();
            } else {
                //TODO corner case handling
            }
            method.nextControlFlow("catch ($T e)", Exception.class)
                    .addStatement("throw $T.wrapWithPath(e, $T.class, fieldName)",
                            JsonMappingException.class,
                            clazz)
                    .endControlFlow()
                    .addStatement("p.nextToken()");
        }
        method.endControlFlow();

        method.addCode("return ");
        ExecutableElement jsonCreator = autoClass.getJsonCreator();
        if (jsonCreator != null) {
            method.addCode("$T.$N(", autoClass.getType(), autoClass.getJsonCreator().getSimpleName());
            List<Property> properties = autoClass.getProperties();
            for (int i = 0; i < properties.size(); i++) {
                Property property = properties.get(i);
                if (i != 0) method.addCode(", ");
                method.addCode("$N", property.name());
            }
            method.addStatement(")");
        } else {
            method.addCode(AutoValueUtil.newFinalClassConstructorCall(context, autoClass.allKeys()));
        }

        return TypeSpec.classBuilder(deserializerName)
                .superclass(deserializerType)
                .addSuperinterface(ResolvableDeserializer.class)
                .addModifiers(Modifier.STATIC, Modifier.FINAL)
                .addFields(emitTypeParamFields(autoClass))
                .addFields(emitPropertyDeserializers(autoClass))
                .addMethod(emitConstructor(autoClass, clazz, env))
                .addMethod(emitResolveMethod(autoClass, env))
                .addFields(emitDefaultFields(autoClass))
                .addMethods(emitDefaultSetters(autoClass, deserializerName))
                .addMethod(method.build())
                .build();
    }

    private static MethodSpec emitResolveMethod(AutoClass autoClass, ProcessingEnvironment env) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolve")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(DeserializationContext.class, "ctxt")
                .addException(JsonMappingException.class)
                .addStatement("$T tf = ctxt.getTypeFactory()", TypeFactory.class);

        for (Property property : autoClass.getProperties()) {
            if (!(property.type() instanceof PrimitiveType)) {
                builder.addCode("$T $NType = ", JavaType.class, property.name());
                builder.addCode(constructType(property.type(), autoClass, env.getTypeUtils()));
                builder.addStatement("");
                builder.addStatement("this.$NDeserializer = ctxt.findRootValueDeserializer($NType)",
                        property.name(),
                        property.name());
            }
        }

        return builder.build();
    }

    private static List<FieldSpec> emitPropertyDeserializers(AutoClass autoClass) {
        List<FieldSpec> fields = new ArrayList<>();
        for (Property property : autoClass.getProperties()) {
            if (!(property.type() instanceof PrimitiveType)) {
                fields.add(FieldSpec.builder(
                        JsonDeserializer.class
                        , property.name() + "Deserializer", Modifier.PRIVATE).build());
            }
        }
        return fields;
    }

    @NotNull
    private static MethodSpec emitConstructor(AutoClass autoClass, TypeMirror clazz, ProcessingEnvironment env) {
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
        for (TypeParameterElement parameterElement : autoClass.getTypeParams()) {
            constructorBuilder.addParameter(JavaType.class, "type" + parameterElement.getSimpleName());
        }
        constructorBuilder.addStatement("super($T.class)", clazz);
        for (TypeParameterElement parameterElement : autoClass.getTypeParams()) {
            Name name = parameterElement.getSimpleName();
            constructorBuilder.addStatement("this.typeParam$N = type$N", name, name);
        }

        return constructorBuilder.build();
    }

    private static CodeBlock constructType(TypeMirror type, AutoClass autoClass, Types types) {
        if (type.getKind() == TypeKind.ARRAY) {
            ArrayType arrayType = MoreTypes.asArray(type);
            TypeMirror componentType = arrayType.getComponentType();
            return CodeBlock.builder()
                    .add("tf.constructArrayType(")
                    .add(constructType(componentType, autoClass, types))
                    .add(")")
                    .build();
        } else if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = MoreTypes.asDeclared(type);
            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (typeArguments.isEmpty()) {
                return CodeBlock.of("tf.constructType($T.class)", type);
            } else {
                CodeBlock.Builder builder = CodeBlock.builder()
                        .add("tf.constructParametricType($T.class", types.erasure(declaredType));
                for (TypeMirror argument : typeArguments) {
                    builder.add(", ")
                            .add(constructType(argument, autoClass, types));
                }
                return builder
                        .add(")")
                        .build();
            }
        } else if (type.getKind() == TypeKind.TYPEVAR) {
            TypeVariable typeVariable = MoreTypes.asTypeVariable(type);
            for (TypeParameterElement typeParam : autoClass.getTypeParams()) {
                if (typeParam.equals(typeVariable.asElement())) {
                    return CodeBlock.of("typeParam" + typeParam.getSimpleName());
                }
            }
            return CodeBlock.of("UNKNOWN");
        } else {
            return CodeBlock.of("tf.constructType($T.class)", type);
        }
    }

    private static List<FieldSpec> emitTypeParamFields(AutoClass autoClass) {
        List<FieldSpec> fields = new ArrayList<>();
        for (TypeParameterElement parameterElement : autoClass.getTypeParams()) {
            fields.add(FieldSpec.builder(JavaType.class, "typeParam" + parameterElement.getSimpleName(), Modifier.PRIVATE, Modifier.FINAL)
                    .build());
        }
        return fields;
    }

    private static Iterable<MethodSpec> emitDefaultSetters(AutoClass autoClass, String deserializerName) {
        List<MethodSpec> specs = new ArrayList<>();
        for (Property property : autoClass.getProperties()) {
            MethodSpec setter = MethodSpec.methodBuilder(Utils.getDefaultSetterName(property))
                    .returns(ClassName.bestGuess(deserializerName))
                    .addParameter(Utils.upperType(property.type()), property.name())
                    .addStatement("this.$N = $N", Utils.getDefaultVarName(property.name()), property.name())
                    .addStatement("return this")
                    .build();
            specs.add(setter);
        }
        return specs;
    }

    private static Iterable<FieldSpec> emitDefaultFields(AutoClass autoClass) {
        List<FieldSpec> fields = new ArrayList<>();
        for (Property property : autoClass.getProperties()) {
            FieldSpec field = FieldSpec.builder(Utils.upperType(property.type()), Utils.getDefaultVarName(property.name()), Modifier.PRIVATE)
                    .build();
            fields.add(field);
        }
        return fields;
    }

}
