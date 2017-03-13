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
        CodeBlock serialize(TypeMirror type, String propName);
    }

    private SerializerStrategy[] strategies = new SerializerStrategy[]{
            primitive(TypeKind.BOOLEAN, "gen.writeBoolean(value.$N())"),
            primitiveNumber(TypeKind.BYTE, TypeKind.SHORT, TypeKind.INT),
            customClass(String.class, "gen.writeString(value.$N())"),
            fallback()
    };

    private SerializerStrategy customClass(final Class<?> clazz, final String code) {
        return new SerializerStrategy() {
            @Override
            public CodeBlock serialize(TypeMirror type, String propName) {
                if (MoreTypes.isTypeOf(clazz, type)) {
                    return CodeBlock.of(code, propName);
                }
                return null;
            }
        };
    }

    private SerializerStrategy primitiveNumber(final TypeKind... kinds) {
        final Set<TypeKind> kindSet = new HashSet<>(Arrays.asList(kinds));
        return new SerializerStrategy() {
            @Override
            public CodeBlock serialize(TypeMirror type, String propName) {
                if (kindSet.contains(type.getKind())) {
                    return CodeBlock.of("gen.writeNumber(value.$N())", propName);
                }
                return null;
            }
        };
    }

    private SerializerStrategy fallback() {
        return new SerializerStrategy() {
            @Override
            public CodeBlock serialize(TypeMirror type, String propName) {
                return CodeBlock.of("gen.writeObject(value.$N())", propName);
            }
        };
    }

    private SerializerStrategy primitive(final TypeKind kind, final String method) {
        return new SerializerStrategy() {
            @Override
            public CodeBlock serialize(TypeMirror type, String propname) {
                if (type.getKind() == kind) {
                    return CodeBlock.of(method, propname);
                }
                return null;
            }
        };
    }

    CodeBlock serialize(Property property) {
        for (SerializerStrategy strategy : strategies) {
            CodeBlock block = strategy.serialize(property.type(), property.key());
            if (block != null) {
                return block;
            }
        }
        throw new IllegalStateException("cannot serialize " + property.type());
    }
}
