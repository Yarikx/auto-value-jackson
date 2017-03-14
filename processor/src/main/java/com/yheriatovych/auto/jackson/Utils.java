package com.yheriatovych.auto.jackson;

import com.yheriatovych.auto.jackson.model.Property;
import org.jetbrains.annotations.NotNull;

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

}
