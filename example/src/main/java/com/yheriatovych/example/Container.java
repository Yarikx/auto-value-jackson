package com.yheriatovych.example;

import com.fasterxml.jackson.databind.Module;
import com.google.auto.value.AutoValue;

/**
 * Created by yaroslav.heriatovych on 6/7/17.
 */
@AutoValue
public abstract class Container<T> {
    public abstract T content();

    public static Module module() {
        return null;
    }
}
