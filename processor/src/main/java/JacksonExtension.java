import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.gabrielittner.auto.value.util.AutoValueUtil;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.squareup.javapoet.*;
import com.yheriatovych.auto.jackson.AutoJackson;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.Map;

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
                .addType(emitSerializer(context))
                .addType(emitDeserializer(context))
                .build();
        return JavaFile.builder(context.packageName(), typeSpec)
                .skipJavaLangImports(true)
                .build()
                .toString();
    }

    private TypeSpec emitDeserializer(Context context) {
        ParameterizedTypeName deserializerType = ParameterizedTypeName.get(
                ClassName.get(JsonDeserializer.class),
                ClassName.get(context.autoValueClass())
        );
        MethodSpec.Builder method = MethodSpec.methodBuilder("deserialize")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(JsonParser.class, "p")
                .addParameter(DeserializationContext.class, "ctxt")
                .addException(IOException.class)
                .addException(JsonProcessingException.class)
                .returns(ClassName.get(context.autoValueClass()));

        method.beginControlFlow("if (p.getCurrentToken() == $T.START_OBJECT)", JsonToken.class)
                .addStatement("p.nextToken()")
                .endControlFlow();

        Map<String, ExecutableElement> properties = context.properties();
        for (String key : properties.keySet()) {
            ExecutableElement executableElement = properties.get(key);
            TypeMirror returnType = executableElement.getReturnType();
            method.addStatement("$T $N = $L", returnType, key, getDefault(returnType));
        }

        //while loop
        method.beginControlFlow("while (p.getCurrentToken() != $T.END_OBJECT) ", JsonToken.class);
        {
            //TODO check if token is field name
            method.addStatement("String fieldName = p.getCurrentName()");
            method.addStatement("p.nextToken()");

            boolean isFirst = true;
            for (String key : properties.keySet()) {
                if (isFirst) {
                    method.beginControlFlow("if (fieldName.equals($S))", key);
                } else {
                    method.nextControlFlow("else if (fieldName.equals($S))", key);
                }
                isFirst = false;

                method.addCode("$N = ", key);
                method.addCode(getGetterForType(properties.get(key).getReturnType()));
                method.addCode(";\n");
            }
            method.endControlFlow();

            method.addStatement("p.nextToken()");
        }
        method.endControlFlow();

        method.addCode("return ");
        method.addCode(AutoValueUtil.newFinalClassConstructorCall(context, properties.keySet().toArray(new Object[properties.size()])));

        return TypeSpec.classBuilder("JacksonDeserializer")
                .superclass(deserializerType)
                .addModifiers(Modifier.STATIC, Modifier.FINAL)
                .addMethod(method.build())
                .build();
    }

    private CodeBlock getGetterForType(TypeMirror returnType) {
        switch (returnType.getKind()) {
            default:

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
                return "0";
            case CHAR:
                return "'\0'";
            case FLOAT:
            case DOUBLE:
                return "0.";
            default:
                return "null";
        }
    }

    private TypeSpec emitSerializer(Context context) {
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
        return context.autoValueClass().getAnnotation(AutoJackson.class) != null;
    }
}
