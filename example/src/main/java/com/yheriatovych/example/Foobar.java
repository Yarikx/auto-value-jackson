package com.yheriatovych.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.Module;
import com.google.auto.value.AutoValue;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class Foobar {

    @JsonProperty("custom_foo") 
    public abstract String foo();
    public abstract int bar();
    public abstract List<String> items();
    public abstract boolean wtf();
    public abstract Date timestamp();

    @JsonCreator
    public static Foobar create(String foo, int bar, List<String> items, boolean wtf, Date timestamp) {
        return new AutoValue_Foobar(foo, bar, items, wtf, timestamp);
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

        public abstract Builder wtf(boolean wtf);

        public abstract Builder timestamp(Date timestamp);

        public abstract Foobar build();
    }
}
