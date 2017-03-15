package com.yheriatovych.auto.jackson;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.value.AutoValue;
import org.intellij.lang.annotations.Language;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class BeanNamesTest {
    @AutoValue
    static abstract class TestBean {
        public abstract String getFoo();
        public abstract int getBar();
        public abstract boolean getBaz();

        public static TestBean create(String newFoo, int newBar, boolean newBaz) {
            return new AutoValue_BeanNamesTest_TestBean(newFoo, newBar, newBaz);
        }

        public static Module module() {
            return new AutoValue_BeanNamesTest_TestBean.JacksonModule();
        }
    }

    @Test
    public void beanSerializeDeserialize() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(TestBean.module());

        TestBean bean = TestBean.create("foo", 42, true);

        String json = mapper.writeValueAsString(bean);
        @Language("JSON") String expectedJson = "{\"foo\":\"foo\",\"bar\":42,\"baz\":true}";
        Assert.assertEquals(expectedJson, json);

        TestBean deserialized = mapper.readValue(json, TestBean.class);
        Assert.assertEquals(bean, deserialized);
    }
}
