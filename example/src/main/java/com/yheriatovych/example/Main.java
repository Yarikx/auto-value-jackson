package com.yheriatovych.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.intellij.lang.annotations.Language;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public class Main {

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(Foobar.jacksonModule());

        Foobar foobar = Foobar.builder()
                .foo("foo")
                .bar(42)
                .items(Arrays.asList("foo", "bar"))
                .wtf(true)
                .timestamp(new Date())
                .build();
        System.out.println(foobar);

        String json = mapper.writeValueAsString(foobar);
        System.out.println(json);
        Foobar readed = mapper.readValue(json, Foobar.class);
        System.out.println(readed);

        mapper.registerModule(Container.module());
        @Language("JSON") String json2 = "{\"content\": [\"foobar\"]}";
        Object value = mapper.readValue(json2, new TypeReference<Container<String>>(){});
        System.out.println(value);
        System.out.println(value.equals(Container.create(Collections.singletonList("foobar"))));
    }
}
