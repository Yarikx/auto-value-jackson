package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.value.AutoValue;
import org.intellij.lang.annotations.Language;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class EmptyClassTest {

    @AutoValue
    static abstract class EmptyData {
        static Module module() {
            return new AutoValue_EmptyClassTest_EmptyData.JacksonModule();
        }

        public static EmptyData create() {
            return new AutoValue_EmptyClassTest_EmptyData();
        }
    }

    private ObjectMapper mapper;

    @Before
    public void setup(){
        mapper = new ObjectMapper();
        mapper.registerModule(EmptyData.module());
    }

    @Test
    public void serializeEmpty() throws JsonProcessingException {
        String json = mapper.writeValueAsString(EmptyData.create());
        Assert.assertEquals("{}", json);
    }

    @Test
    public void deserializeEmpty() throws IOException {
        EmptyData emptyData = mapper.readValue("{}", EmptyData.class);
        Assert.assertEquals(EmptyData.create(), emptyData);
    }

    @Test
    public void deserializeEmptyWithIgnoredValues() throws IOException {
        @Language("JSON") String json = "{\n" +
                "  \"foo\": 42,\n" +
                "  \"bar\": {\n" +
                "    \"a\": \"a\",\n" +
                "    \"b\": 3\n" +
                "  }\n" +
                "}";
        EmptyData emptyData = mapper.readValue(json, EmptyData.class);
        Assert.assertEquals(EmptyData.create(), emptyData);
    }
}
