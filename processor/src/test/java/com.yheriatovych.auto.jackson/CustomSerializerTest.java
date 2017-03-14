package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import org.intellij.lang.annotations.Language;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class CustomSerializerTest {


    public static class YesNoSerializer extends JsonSerializer<Boolean> {
        @Override
        public void serialize(Boolean value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value ? "yes" : "no");
        }
    }

    @AutoValue
    protected static abstract class TestData {
        @JsonSerialize(using = YesNoSerializer.class)
        abstract boolean answer();

        public static TestData create(boolean answer) {
            return new AutoValue_CustomSerializerTest_TestData(answer);
        }

        public static Module module() {
            return new AutoValue_CustomSerializerTest_TestData.JacksonModule();
        }
    }

    private ObjectMapper mapper;

    @Before
    public void setup() {
        mapper = new ObjectMapper();
        mapper.registerModule(TestData.module());
    }

    @Test
    public void useCustomSerializer() throws JsonProcessingException {
        TestData testData = TestData.create(true);
        String json = mapper.writeValueAsString(testData);
        @Language("JSON") String expected = "{\"answer\":\"yes\"}";
        Assert.assertEquals(expected, json);
    }
}
