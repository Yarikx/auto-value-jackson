import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.value.AutoValue;
import com.sun.istack.internal.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class PolymorphicTest {

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
//            visible = true,
            property = "type")
    @JsonSubTypes(
            {
                    @JsonSubTypes.Type(value = Dog.class),
                    @JsonSubTypes.Type(value = Cat.class),
                    @JsonSubTypes.Type(value = Bird.class),
            }
    )
    interface Animal {}

    @JsonTypeName("dog")
    static class Dog implements Animal{}
    @JsonTypeName("cat")
    static class Cat implements Animal{}

    @AutoValue
    @JsonTypeName("bird")
    static abstract class Bird implements Animal {
        @Nullable
        public abstract String foobar();

        public static Bird create(String foobar) {
            return new AutoValue_PolymorphicTest_Bird(foobar);
        }

        public static Module birdModule() {
            return new AutoValue_PolymorphicTest_Bird.JacksonModule();
        }
    }

    private ObjectMapper mapper;

    @Before
    public void setup() {
        mapper = new ObjectMapper();
        mapper.enable(JsonGenerator.Feature.IGNORE_UNKNOWN);
        mapper.enable(JsonParser.Feature.IGNORE_UNDEFINED);
        mapper.registerModule(Bird.birdModule());
    }

    @Test
    public void simpleDeserializeDog() throws IOException {
        String json = "{\"type\":\"dog\"}";
        Animal animal = mapper.readValue(json, Animal.class);
        Assert.assertEquals(Dog.class, animal.getClass());
    }

    @Test
    public void simpleDeserializeCat() throws IOException {
        String json = "{\"type\":\"cat\"}";
        Animal animal = mapper.readValue(json, Animal.class);
        Assert.assertEquals(Cat.class, animal.getClass());
    }

    @Test
    public void simpleSerializeDog() throws IOException {
        String json = mapper.writeValueAsString(new Dog());
        String expected = "{\"type\":\"dog\"}";
        Assert.assertEquals(expected, json);
    }

    @Test
    public void simpleSerializeCat() throws IOException {
        String json = mapper.writeValueAsString(new Cat());
        String expected = "{\"type\":\"cat\"}";
        Assert.assertEquals(expected, json);
    }


    //*************

    @Test
    public void simpleDeserializeBird() throws IOException {
        String json = "{\"type\":\"bird\", \"foobar\":\"qwe\"}";
        Animal animal = mapper.readValue(json, Bird.class);

        Assert.assertEquals(Bird.create("qwe"), animal);
        Assert.assertTrue(Bird.class.isAssignableFrom(animal.getClass()));
        Assert.assertTrue(AutoValue_PolymorphicTest_Bird.class.isAssignableFrom(animal.getClass()));
    }

    @Test
    public void simpleSerializeBird() throws IOException {
        Bird bird = Bird.create("qwe");
        String json = mapper.writeValueAsString(bird);
        String expected = "{\"type\":\"bird\",\"foobar\":\"qwe\"}";
        Assert.assertEquals(expected, json);
    }
}
