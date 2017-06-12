package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;
import com.google.auto.value.AutoValue;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by yaroslav.heriatovych on 6/2/17.
 */
public class GenericsTest {

    private ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(TestData.jacksonModule());
    }

    @AutoValue
    protected static abstract class TestData<T> {
        abstract T foo();
        abstract List<T> bar();

        static Module jacksonModule() {
            return new AutoValue_GenericsTest_TestData.JacksonModule();
        }
        static <T> TestData<T> create(T foo, List<T> bar) {
            return new AutoValue_GenericsTest_TestData(foo, bar);
        }
    }

    @Test
    public void serializeGenericClass() throws JsonProcessingException {
        TestData<String> data = TestData.create("foobar", Arrays.asList("foo", "bar"));
        String json = mapper.writeValueAsString(data);

        @Language("JSON") String expected = "{\"foo\":\"foobar\",\"bar\":[\"foo\",\"bar\"]}";
        assertEquals(expected, json);
    }

    @Test
    public void deserializeSimpleGenericClass() throws IOException {
        @Language("JSON") String json = "{\"foo\":\"foobar\",\"bar\":[\"foo\",\"bar\"]}";
        TestData data = mapper.readValue(json, new TypeReference<TestData<String>>() {});

        TestData<String> expected = TestData.create("foobar", Arrays.asList("foo", "bar"));
        assertEquals(expected, data);
    }
}
