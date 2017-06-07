package com.yheriatovych.auto.jackson.deserializer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.auto.common.MoreTypes;
import com.squareup.javapoet.CodeBlock;
import com.yheriatovych.auto.jackson.model.AutoClass;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Date;

class DeserializerDispatcher {
    private final AutoClass autoClass;

    public DeserializerDispatcher(AutoClass autoClass) {

        this.autoClass = autoClass;
    }

    interface DeserStrategy {
        CodeBlock deser(TypeMirror type);
    }

    private final DeserStrategy[] strategies = new DeserStrategy[]{
            primitive(TypeKind.BOOLEAN, "_parseBooleanPrimitive(p, ctxt)"),
            primitive(TypeKind.BYTE, "_parseBytePrimitive(p, ctxt)"),
            primitive(TypeKind.SHORT, "_parseShortPrimitive(p, ctxt)"),
            primitive(TypeKind.INT, "_parseIntPrimitive(p, ctxt)"),
            primitive(TypeKind.LONG, "_parseLongPrimitive(p, ctxt)"),
            primitive(TypeKind.FLOAT, "_parseFloatPrimitive(p, ctxt)"),
            primitive(TypeKind.DOUBLE, "_parseDoublePrimitive(p, ctxt)"),
            fallbackStrategy()
    };

    private DeserStrategy fallbackStrategy() {
        return new DeserStrategy() {
            @Override
            public CodeBlock deser(TypeMirror type) {
                if (type.getKind() == TypeKind.DECLARED) {
                    DeclaredType declaredType = MoreTypes.asDeclared(type);
                    if (declaredType.getTypeArguments().size() > 0) {
                        return CodeBlock.of("p.readValueAs(new $T<$T>(){})", TypeReference.class, type);
                    }
                }

                return CodeBlock.of("p.readValueAs($T.class)", type);
            }
        };
    }

    private DeserStrategy primitive(final TypeKind kind, final String method) {
        return new DeserStrategy() {
            @Override
            public CodeBlock deser(TypeMirror type) {
                if (type.getKind() == kind) {
                    return CodeBlock.of(method);
                }
                return null;
            }
        };
    }

    CodeBlock deser(TypeMirror type) {
        for (DeserStrategy strategy : strategies) {
            CodeBlock block = strategy.deser(type);
            if (block != null) {
                return block;
            }
        }
        throw new IllegalStateException("Do not know how to handle " + type);
    }
}
