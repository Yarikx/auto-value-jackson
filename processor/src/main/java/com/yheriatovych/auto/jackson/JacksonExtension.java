package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.databind.Module;
import com.gabrielittner.auto.value.util.AutoValueUtil;
import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.yheriatovych.auto.jackson.deserializer.DeserializerEmitter;
import com.yheriatovych.auto.jackson.model.AutoClass;
import com.yheriatovych.auto.jackson.serializer.SerializerEmitter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

@AutoService(AutoValueExtension.class)
public class JacksonExtension extends AutoValueExtension {
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

    @Override
    public String generateClass(Context context, String className, String classToExtend, boolean isFinal) {
        TypeSpec.Builder builder = AutoValueUtil.newTypeSpecBuilder(context, className, classToExtend, isFinal);

        String serializerName = "JacksonSerializer";
        String deserializerName = "JacksonDeserializer";
        String moduleName = "JacksonModule";

        AutoClass autoClass = AutoClass.parse(context);

        TypeSpec typeSpec = builder
                .addType(SerializerEmitter.emitSerializer(autoClass, serializerName, context.processingEnvironment()))
                .addType(DeserializerEmitter.emitDeserializer(autoClass, context, deserializerName))
                .addType(ModuleEmitter.emitModule(autoClass, serializerName, deserializerName, moduleName, context.processingEnvironment()))
                .build();
        return JavaFile.builder(context.packageName(), typeSpec)
                .skipJavaLangImports(true)
                .build()
                .toString();
    }

}
