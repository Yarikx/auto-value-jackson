package com.yheriatovych.auto.jackson.deserializer;

import com.fasterxml.jackson.core.JsonToken;
import com.squareup.javapoet.CodeBlock;
import com.yheriatovych.auto.jackson.Utils;
import com.yheriatovych.auto.jackson.model.AutoClass;
import com.yheriatovych.auto.jackson.model.Property;

import javax.lang.model.type.TypeKind;

class DeserializerDispatcher {
    private final AutoClass autoClass;

    public DeserializerDispatcher(AutoClass autoClass) {

        this.autoClass = autoClass;
    }

    interface DeserStrategy {
        CodeBlock deser(Property property);
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
            public CodeBlock deser(Property property) {
                return CodeBlock.of("($T) " +
                                "(p.getCurrentToken() == $T.VALUE_NULL" +
                                " ? $NDeserializer.getNullValue(ctxt)" +
                                " : $NDeserializer.deserialize(p, ctxt))",
                        Utils.upperType(property.type()), JsonToken.class, property.name(), property.name());
            }
        };
    }

    private DeserStrategy primitive(final TypeKind kind, final String method) {
        return new DeserStrategy() {
            @Override
            public CodeBlock deser(Property property) {
                if (property.type().getKind() == kind) {
                    return CodeBlock.of(method);
                }
                return null;
            }
        };
    }

    CodeBlock deser(Property type) {
        for (DeserStrategy strategy : strategies) {
            CodeBlock block = strategy.deser(type);
            if (block != null) {
                return block;
            }
        }
        throw new IllegalStateException("Do not know how to handle " + type);
    }
}
