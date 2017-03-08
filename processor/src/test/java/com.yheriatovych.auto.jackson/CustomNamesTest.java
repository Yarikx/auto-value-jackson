package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.value.AutoValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class CustomNamesTest {
    @AutoValue
    protected static abstract class TestData {
        @JsonProperty("custom_foo")
        abstract String foo();
        @JsonProperty("custom_bar")
        abstract boolean bar();

        public static TestData create(String foo, boolean bar) {
            return new AutoValue_CustomNamesTest_TestData(foo, bar);
        }


        public static Module module() {
            return new AutoValue_CustomNamesTest_TestData.JacksonModule();
        }
    }

    private ObjectMapper mapper;

    @Before
    public void setup() {
        mapper = new ObjectMapper();
        mapper.registerModule(TestData.module());
    }

    @Test
    public void serializeWithCustomPropertyNames() throws JsonProcessingException {
        TestData testData = TestData.create("123", true);
        String json = mapper.writeValueAsString(testData);
        Assert.assertEquals("{\"custom_foo\":\"123\",\"custom_bar\":true}", json);
    }

    @Test
    public void deserializeWithCustomPropertyNames() throws IOException {
        String json = "{\"custom_foo\":\"123\",\"custom_bar\":true}";
        TestData data = mapper.readValue(json, TestData.class);
        TestData expected = TestData.create("123", true);
        Assert.assertEquals(expected, data);
    }
}
