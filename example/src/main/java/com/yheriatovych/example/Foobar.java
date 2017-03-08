package com.yheriatovych.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.Module;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.util.List;

@AutoValue
public abstract class Foobar {

    @JsonProperty("custom_foo")
    public abstract String foo();
    public abstract int bar();
    public abstract List<String> items();

    public static Foobar create(String foo, int bar, List<String> items) {
        return builder()
                .foo(foo)
                .bar(bar)
                .items(items)
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

        public abstract Builder items(List<String> items);

        public abstract Foobar build();
    }
}
