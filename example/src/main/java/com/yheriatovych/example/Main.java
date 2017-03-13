package com.yheriatovych.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(Foobar.jacksonModule());

        Foobar foobar = Foobar.builder()
                .foo("foo")
                .bar(42)
                .items(Arrays.asList("foo", "bar"))
                .wtf(true)
                .build();

        String json = mapper.writeValueAsString(foobar);
        System.out.println(json);
        Foobar readed = mapper.readValue(json, Foobar.class);
        System.out.println(readed);
    }
}
