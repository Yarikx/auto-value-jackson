package com.yheriatovych.example;

import com.fasterxml.jackson.databind.Module;
import com.google.auto.value.AutoValue;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Created by yaroslav.heriatovych on 6/7/17.
 */
@AutoValue
public abstract class Container<T extends Serializable> {
    public abstract List<T> content();

    public static <T extends Serializable> Container<T> create(List<T> content) {
        return new AutoValue_Container<>(content);
    }

    public static Module module() {
        return new AutoValue_Container.JacksonModule()
                .setDefaultContent(Collections.emptyList());
    }
}
