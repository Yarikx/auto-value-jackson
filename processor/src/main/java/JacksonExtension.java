import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.gabrielittner.auto.value.util.AutoValueUtil;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.squareup.javapoet.*;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.io.IOException;

@AutoService(AutoValueExtension.class)
public class JacksonExtension extends AutoValueExtension {
    @Override
    public String generateClass(Context context, String className, String classToExtend, boolean isFinal) {
        TypeSpec.Builder builder = AutoValueUtil.newTypeSpecBuilder(context, className, classToExtend, isFinal);

        MethodSpec.Builder serializer = MethodSpec.methodBuilder("serializer")
                .addModifiers(Modifier.STATIC)
                .returns(ParameterizedTypeName.get(
                        ClassName.get(JsonSerializer.class),
                        ClassName.get(context.autoValueClass())
                ));

        TypeSpec typeSpec = builder
                .addType(emitSerializerCode(context))
                .build();
        return JavaFile.builder(context.packageName(), typeSpec)
                .skipJavaLangImports(true)
                .build()
                .toString();
    }

    private TypeSpec emitSerializerCode(Context context) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("serialize")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(context.autoValueClass()), "value")
                .addParameter(JsonGenerator.class, "gen")
                .addParameter(SerializerProvider.class, "serializers")
                .addException(IOException.class)
                .addException(JsonProcessingException.class);

        method.addStatement("gen.writeStartObject()");
        for (String key : context.properties().keySet()) {
            ExecutableElement element = context.properties().get(key);
            method.addStatement("gen.writeFieldName($S)", key);
            method.addStatement("gen.writeObject(value.$N())", key);
        }
        method.addStatement("gen.writeEndObject()");

        return TypeSpec.classBuilder("JacksonSerializer")
                .superclass(ParameterizedTypeName.get(
                        ClassName.get(JsonSerializer.class),
                        ClassName.get(context.autoValueClass())
                ))
                .addModifiers(Modifier.STATIC, Modifier.FINAL)
                .addMethod(method.build())
                .build();
    }

    @Override
    public boolean applicable(Context context) {
        return true;
    }
}
