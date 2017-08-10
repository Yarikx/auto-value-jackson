package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.value.AutoValue;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonCreatorTest {

    private ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(TestData.jacksonModule());
    }

    @AutoValue
    protected static abstract class TestData {
        abstract String foo();
        abstract int bar();

        boolean creatorCalled = false;

        static Module jacksonModule() {
            return new AutoValue_JsonCreatorTest_TestData.JacksonModule();
        }

        @JsonCreator
        static TestData create(String foo, int bar) {
            TestData data = new AutoValue_JsonCreatorTest_TestData(foo, bar);
            data.creatorCalled = true;
            return data;
        }
    }

    @Test
    public void DeserializeSimpleObject() throws IOException {
        String json = "{\"foo\":\"test\",\"bar\":4}";
        TestData actual = mapper.readValue(json, TestData.class);
        assertTrue(actual.creatorCalled);
    }

}
