package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.intellij.lang.annotations.Language;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class CustomDeserializerTest {


    public static class YesNoDeserializer extends JsonDeserializer<Boolean> {
        @Override
        public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            String text = p.getText();
            if("yes".equals(text)) {
                return true;
            } else if("no".equals(text)) {
                return false;
            } else {
                throw new JsonParseException(p, String.format("yes/no expected, %s found", text));
            }
        }
    }

    @AutoValue
    protected static abstract class TestData {
        @JsonDeserialize(using = YesNoDeserializer.class)
        abstract boolean answer();

        public static TestData create(boolean answer) {
            return new AutoValue_CustomDeserializerTest_TestData(answer);
        }

        public static Module module() {
            return new AutoValue_CustomDeserializerTest_TestData.JacksonModule();
        }
    }

    private ObjectMapper mapper;

    @Before
    public void setup() {
        mapper = new ObjectMapper();
        mapper.registerModule(TestData.module());
    }

    @Test
    public void useCustomDeserializer() throws IOException {
        @Language("JSON") String json = "{\"answer\":\"yes\"}";
        TestData expected = TestData.create(true);
        TestData actual = mapper.readValue(json, TestData.class);

        Assert.assertEquals(expected, actual);
    }
}
