package com.yheriatovych.example;

import com.fasterxml.jackson.databind.Module;
import com.google.auto.value.AutoValue;

import java.lang.ref.Reference;
import java.util.List;

/**
 * Created by yaroslav.heriatovych on 6/7/17.
 */
@AutoValue
public abstract class Container<T> {
    public abstract T content();
    public abstract List<T> wrappedContent();
    public abstract List<Reference<T>> wrappedContentTwice();
    public abstract Reference<T[]> wrappedContentWithArray();

//    public static <T> Container<T> create(T content) {
//        return new AutoValue_Container<>(content);
//    }

    public static Module module() {
        return new AutoValue_Container.JacksonModule();
    }
}
