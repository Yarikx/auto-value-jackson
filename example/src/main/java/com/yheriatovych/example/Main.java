package com.yheriatovych.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        mapper.registerModule(module);

        String json = "{\"foo\":\"test\",\"bar\":4}";
        Foobar foobar = mapper.readValue(json, Foobar.class);
        System.out.println(foobar);
    }
}
