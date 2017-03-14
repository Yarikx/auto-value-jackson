package com.yheriatovych.auto.jackson.serializer;

import com.google.auto.common.MoreTypes;
import com.squareup.javapoet.CodeBlock;
import com.yheriatovych.auto.jackson.model.Property;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class SerializerDispatcher {
    interface SerializerStrategy {
        CodeBlock serialize(Property property);
    }

    private SerializerStrategy[] strategies = new SerializerStrategy[]{
            customSerializer(),
            primitive(TypeKind.BOOLEAN, "gen.writeBoolean(value.$N())"),
            primitiveNumber(TypeKind.BYTE, TypeKind.SHORT, TypeKind.INT),
            customClass(String.class, "gen.writeString(value.$N())"),
            fallback()
    };

    private SerializerStrategy customSerializer() {
        return new SerializerStrategy() {
            @Override
            public CodeBlock serialize(Property property) {
                TypeMirror customSerializer = property.customSerializer();
                if (customSerializer != null) {
                    return CodeBlock.of("serializers.serializerInstance(null, $T.class)" +
                            ".serialize(value.$N(), gen, serializers)", customSerializer, property.name());
                }
                return null;
            }
        };
    }

    private SerializerStrategy customClass(final Class<?> clazz, final String code) {
        return new SerializerStrategy() {
            @Override
            public CodeBlock serialize(Property property) {
                if (MoreTypes.isTypeOf(clazz, property.type())) {
                    return CodeBlock.of(code, property.name());
                }
                return null;
            }
        };
    }

    private SerializerStrategy primitiveNumber(final TypeKind... kinds) {
        final Set<TypeKind> kindSet = new HashSet<>(Arrays.asList(kinds));
        return new SerializerStrategy() {
            @Override
            public CodeBlock serialize(Property property) {
                if (kindSet.contains(property.type().getKind())) {
                    return CodeBlock.of("gen.writeNumber(value.$N())", property.name());
                }
                return null;
            }
        };
    }

    private SerializerStrategy fallback() {
        return new SerializerStrategy() {
            @Override
            public CodeBlock serialize(Property property) {
                return CodeBlock.of("gen.writeObject(value.$N())", property.name());
            }
        };
    }

    private SerializerStrategy primitive(final TypeKind kind, final String method) {
        return new SerializerStrategy() {
            @Override
            public CodeBlock serialize(Property property) {
                if (property.type().getKind() == kind) {
                    return CodeBlock.of(method, property.name());
                }
                return null;
            }
        };
    }

    CodeBlock serialize(Property property) {
        for (SerializerStrategy strategy : strategies) {
            CodeBlock block = strategy.serialize(property);
            if (block != null) {
                return block;
            }
        }
        throw new IllegalStateException("cannot serialize " + property.type());
    }
}
