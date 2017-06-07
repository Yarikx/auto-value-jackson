package com.yheriatovych.auto.jackson;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.yheriatovych.auto.jackson.model.Property;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.*;
import java.util.List;

public class Utils {
    private static String capitalizeFirst(String str) {
        //TODO properly handle unicode code points
        char first = Character.toUpperCase(str.charAt(0));
        return first + str.substring(1);
    }

    @NotNull
    public static String getDefaultSetterName(Property property) {
        return "set" + capitalizeFirst(getDefaultVarName(property.name()));
    }

    public static String getDefaultVarName(String property) {
        return "default" + capitalizeFirst(property);
    }

    public static TypeName upperType(TypeMirror type) {
        if(type instanceof PrimitiveType) {
            return TypeName.get(type);
        } else if(type.getKind() == TypeKind.TYPEVAR) {
            TypeVariable typeVariable = MoreTypes.asTypeVariable(type);
            return TypeName.get(typeVariable.getUpperBound());
        } else if(type.getKind() == TypeKind.ARRAY) {
            ArrayType typeVariable = MoreTypes.asArray(type);
            return ArrayTypeName.of(upperType(typeVariable.getComponentType()));
        } else {
            DeclaredType declaredType = MoreTypes.asDeclared(type);
            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if(typeArguments.isEmpty()) {
                return TypeName.get(type);
            } else {
                TypeName[] typeNames = new TypeName[typeArguments.size()];
                for (int i = 0; i < typeArguments.size(); i++) {
                    TypeMirror typeArgument = typeArguments.get(i);
                    typeNames[i] = upperType(typeArgument);
                }
                return ParameterizedTypeName.get(ClassName.get(MoreElements.asType(declaredType.asElement())), typeNames);
            }
        }
    }

}
