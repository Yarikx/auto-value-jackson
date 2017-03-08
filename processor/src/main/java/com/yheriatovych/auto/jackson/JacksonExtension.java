package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.gabrielittner.auto.value.util.AutoValueUtil;
import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.squareup.javapoet.*;
import com.yheriatovych.auto.jackson.model.AutoClass;

import javax.lang.model.element.*;

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
                .addType(SerializerEmitter.emitSerializer(autoClass, serializerName))
                .addType(DeserializerEmitter.emitDeserializer(autoClass, context, deserializerName))
                .addType(emitModule(context, serializerName, deserializerName, moduleName))
                .build();
        return JavaFile.builder(context.packageName(), typeSpec)
                .skipJavaLangImports(true)
                .build()
                .toString();
    }

    private TypeSpec emitModule(Context context, String serializerName, String deserializerName, String moduleName) {
        TypeElement type = context.autoValueClass();
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addStatement("super($S)", type.getSimpleName())
                .addStatement("addSerializer($T.class, new $N())", type, serializerName)
                .addStatement("addDeserializer($T.class, new $N())",type, deserializerName)
                .build();
        return TypeSpec.classBuilder(moduleName)
                .superclass(SimpleModule.class)
                .addModifiers(Modifier.STATIC, Modifier.FINAL)
                .addMethod(constructor)
                .build();
    }
}
