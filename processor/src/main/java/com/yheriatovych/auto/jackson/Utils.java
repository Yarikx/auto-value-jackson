package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.auto.value.extension.AutoValueExtension;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.yheriatovych.auto.jackson.model.Property;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class Utils {
    private static String capitalizeFirst(String str) {
        //TODO properly handle unicode code points
        char first = Character.toUpperCase(str.charAt(0));
        return first + str.substring(1);
    }

    @NotNull
    static String getDefaultSetterName(Property property) {
        return "set" + capitalizeFirst(getDefaultVarName(property.key()));
    }

    static String getDefaultVarName(String property) {
        return "default" + capitalizeFirst(property);
    }

}
