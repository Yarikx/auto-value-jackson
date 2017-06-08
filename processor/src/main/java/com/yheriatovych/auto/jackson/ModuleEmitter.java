package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.squareup.javapoet.*;
import com.sun.tools.javac.jvm.Code;
import com.yheriatovych.auto.jackson.model.AutoClass;
import com.yheriatovych.auto.jackson.model.Property;
import org.jetbrains.annotations.NotNull;

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


        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addStatement("super($S)", type.getSimpleName())
                .addStatement("addSerializer($T.class, new $N())", clazz, serializerName)
                .addStatement("setDeserializers($L)", emitDeserializers(autoClass, deserializerName, env))
                .build();
        return TypeSpec.classBuilder(moduleName)
                .superclass(SimpleModule.class)
                .addModifiers(Modifier.STATIC, Modifier.FINAL)
                .addFields(emitDefaultValues(autoClass))
                .addMethod(constructor)
                .addMethods(emitDefaultSetters(autoClass, moduleName))
                .build();
    }

    @NotNull
    private static TypeSpec emitDeserializers(AutoClass autoClass, String deserializerName, ProcessingEnvironment env) {
        ClassName deserializerClass = ClassName.bestGuess(deserializerName);

        CodeBlock.Builder deserializatoinBlock = CodeBlock.builder();
        CodeBlock typeParamsCheck = autoClass.isGeneric()
                ? CodeBlock.of(" && type.containedTypeCount() == $L", autoClass.getTypeParams().size())
                : CodeBlock.of("");
        CodeBlock.Builder typeArgs = CodeBlock.builder();
        for (int i = 0; i < autoClass.getTypeParams().size(); i++) {
            if(i != 0){
                typeArgs.add(", ");
            }
            typeArgs.add(CodeBlock.of("type.containedType($L)", i));
        }
        deserializatoinBlock.beginControlFlow("if ($T.class.isAssignableFrom(type.getRawClass())$L)", env.getTypeUtils().erasure(autoClass.getType()), typeParamsCheck)
                .addStatement("return new $T($L)", deserializerClass, typeArgs.build())
                .endControlFlow();

        MethodSpec.Builder builder = MethodSpec.methodBuilder("findBeanDeserializer")
                .addModifiers(Modifier.PUBLIC)
                .returns(JsonDeserializer.class)
                .addAnnotation(Override.class)
                .addParameter(JavaType.class, "type")
                .addParameter(DeserializationConfig.class, "config")
                .addParameter(BeanDescription.class, "beanDesc")
                .addException(JsonMappingException.class)
                .addCode(deserializatoinBlock.build())
                .addStatement("return super.findBeanDeserializer(type, config, beanDesc)");


        return TypeSpec.anonymousClassBuilder("")
                .superclass(SimpleDeserializers.class)
                .addMethod(builder.build())
                .build();
    }

    private static List<FieldSpec> emitDefaultValues(AutoClass autoClass) {
        List<FieldSpec> fieldSpecs = new ArrayList<>();
        for (Property property : autoClass.getProperties()) {
            fieldSpecs.add(FieldSpec.builder(Utils.upperType(property.type()), Utils.getDefaultVarName(property.name()), Modifier.PRIVATE).build());
        }
        return fieldSpecs;
    }

    private static Iterable<MethodSpec> emitDefaultSetters(AutoClass autoClass, String moduleName) {
        List<MethodSpec> specs = new ArrayList<>();
        for (Property property : autoClass.getProperties()) {
            String methodName = Utils.getDefaultSetterName(property);
            String argName = property.name();
            MethodSpec setter = MethodSpec.methodBuilder(methodName)
                    .returns(ClassName.bestGuess(moduleName))
                    .addParameter(Utils.upperType(property.type()), argName)
                    .addStatement("this.$N = $N", Utils.getDefaultVarName(property.name()), argName)
                    .addStatement("return this")
                    .build();
            specs.add(setter);
        }
        return specs;
    }
}
