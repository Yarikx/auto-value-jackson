package com.yheriatovych.example;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.yheriatovych.auto.jackson.AutoJackson;

@AutoValue
@AutoJackson
public abstract class Foobar {

    public abstract String foo();
    public abstract int bar();


    public static TypeAdapter<Foobar> typeAdapter(Gson gson) {
        return new AutoValue_Foobar.GsonTypeAdapter(gson);
    }

    public static Foobar create(String foo, int bar) {
        return new AutoValue_Foobar(foo, bar);
    }
}
