package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.value.AutoValue;
import org.intellij.lang.annotations.Language;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by yaroslav.heriatovych on 6/2/17.
 */
public class ExceptionsTest {

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

        static Module jacksonModule() {
            return new AutoValue_ExceptionsTest_TestData.JacksonModule();
        }
        static TestData create(String foo, int bar) {
            return new AutoValue_ExceptionsTest_TestData(foo, bar);
        }
    }

    @Test
    public void exception() throws IOException {
        @Language("JSON") String json = "{\"foo\":1, \"bar\": \"foo\"}";
        try {
            TestData data = mapper.readValue(json, TestData.class);
            fail("should throw JsonMappingException");
        } catch (JsonMappingException jme) {
            List<JsonMappingException.Reference> references = jme.getPath();
            assertEquals(1, references.size());
            JsonMappingException.Reference ref = references.get(0);
            assertEquals(TestData.class, ref.getFrom());
            assertEquals("bar", ref.getFieldName());
        }
    }
}
