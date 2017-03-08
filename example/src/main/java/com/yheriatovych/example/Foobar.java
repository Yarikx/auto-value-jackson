package com.yheriatovych.example;

import com.fasterxml.jackson.databind.Module;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class Foobar {

    public abstract String foo();
    public abstract int bar();

    public static Foobar create(String foo, int bar) {
        return builder()
                .foo(foo)
                .bar(bar)
                .build();
    }

    public static TypeAdapter<Foobar> typeAdapter(Gson gson) {
        return new AutoValue_Foobar.GsonTypeAdapter(gson);
    }
    public static Module jacksonModule() {
        return new AutoValue_Foobar.JacksonModule();
    }

    public static Builder builder() {
        return new AutoValue_Foobar.Builder();
    }


    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder foo(String foo);

        public abstract Builder bar(int bar);

        public abstract Foobar build();
    }
}
