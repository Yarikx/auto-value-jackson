import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.auto.value.AutoValue;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class SimpleTest {

    private ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test");
        module.addSerializer(TestData.class, new AutoValue_SimpleTest_TestData.JacksonSerializer());
        module.addDeserializer(TestData.class, new AutoValue_SimpleTest_TestData.JacksonDeserializer());
        mapper.registerModule(module);
    }

    @AutoValue
    static abstract class TestData {
        abstract String foo();
        abstract int bar();

        public static Module jacksonModule() {
            return new AutoValue_SimpleTest_TestData.JacksonModule();
        }
        static TestData create(String foo, int bar) {
            return new AutoValue_SimpleTest_TestData(foo, bar);
        }
    }

    @Test
    public void serializeSimpleObject() throws JsonProcessingException {
        TestData test = TestData.create("test", 4);
        String json = mapper.writeValueAsString(test);
        assertEquals("{\"foo\":\"test\",\"bar\":4}", json);
    }

    @Test
    public void DeserializeSimpleObject() throws IOException {
        TestData expected = TestData.create("test", 4);
        String json = "{\"foo\":\"test\",\"bar\":4}";
        assertEquals(expected, mapper.readValue(json, TestData.class));
    }

}
