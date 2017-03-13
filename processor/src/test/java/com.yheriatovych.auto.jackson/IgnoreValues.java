package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.value.AutoValue;
import org.intellij.lang.annotations.Language;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class IgnoreValues {

    @AutoValue
    static abstract class TestData {
        abstract int foo();

        static Module module() {
            return new AutoValue_IgnoreValues_TestData.JacksonModule();
        }

        public static TestData create(int foo) {
            return new AutoValue_IgnoreValues_TestData(foo);
        }
    }

    private ObjectMapper mapper;

    @Before
    public void setup(){
        mapper = new ObjectMapper();
        mapper.registerModule(TestData.module());
    }

    @Test
    public void skipUnknownChildrenOnDeserialization() throws IOException {
        @Language("JSON") String json = "{\n" +
                "  \"foo\": 42,\n" +
                "  \"bar\": {\n" +
                "    \"a\": \"a\",\n" +
                "    \"b\": 3\n" +
                "  }\n" +
                "}";

        TestData testData = mapper.readValue(json, TestData.class);
        Assert.assertEquals(TestData.create(42), testData);
    }
}
