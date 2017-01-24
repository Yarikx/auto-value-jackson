package com.yheriatovych.example;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Foobar {
    public abstract String foo();
    public abstract int bar();

    public static Foobar create(String foo, int bar) {
        return new AutoValue_Foobar(foo, bar);
    }
}
