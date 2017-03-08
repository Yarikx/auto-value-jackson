import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.gabrielittner.auto.value.util.AutoValueUtil;
import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.squareup.javapoet.*;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.Map;

@AutoService(AutoValueExtension.class)
public class JacksonExtension extends AutoValueExtension {
    @Override
    public String generateClass(Context context, String className, String classToExtend, boolean isFinal) {
        TypeSpec.Builder builder = AutoValueUtil.newTypeSpecBuilder(context, className, classToExtend, isFinal);

        String serializerName = "JacksonSerializer";
        String deserializerName = "JacksonDeserializer";
        TypeSpec typeSpec = builder
                .addType(emitSerializer(context, serializerName))
                .addType(emitDeserializer(context, deserializerName))
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

    private TypeSpec emitDeserializer(Context context, String deserializerName) {
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

        return TypeSpec.classBuilder(deserializerName)
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

    private TypeSpec emitSerializer(Context context, String serializerName) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("serializeWithType")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(context.autoValueClass()), "value")
                .addParameter(JsonGenerator.class, "gen")
                .addParameter(SerializerProvider.class, "serializers")
                .addParameter(TypeSerializer.class, "typeSer")
                .addException(IOException.class);

        method.beginControlFlow("if (typeSer != null)")
                .addStatement("typeSer.writeTypePrefixForObject(value, gen, $T.class)", context.autoValueClass())
                .nextControlFlow("else")
                .addStatement("gen.writeStartObject()")
                .endControlFlow();
        for (String key : context.properties().keySet()) {
            ExecutableElement element = context.properties().get(key);
            method.addStatement("gen.writeFieldName($S)", key);
            method.addStatement("gen.writeObject(value.$N())", key);
        }
        method.beginControlFlow("if (typeSer != null)")
                .addStatement("typeSer.writeTypeSuffixForObject(value, gen)")
                .nextControlFlow("else")
                .addStatement("gen.writeEndObject()")
                .endControlFlow();

        MethodSpec.Builder simpleSerialize = MethodSpec.methodBuilder("serialize")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(context.autoValueClass()), "value")
                .addParameter(JsonGenerator.class, "gen")
                .addParameter(SerializerProvider.class, "serializers")
                .addException(IOException.class)
                .addException(JsonProcessingException.class)
                .addStatement("this.serializeWithType(value, gen, serializers, null)");

        return TypeSpec.classBuilder(serializerName)
                .superclass(ParameterizedTypeName.get(
                        ClassName.get(JsonSerializer.class),
                        ClassName.get(context.autoValueClass())
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
                        && ee.getModifiers().contains(Modifier.PUBLIC)
                        && MoreTypes.isTypeOf(Module.class, ee.getReturnType())) {
                    return true;
                }
            }
        }
        return false;
    }
}
