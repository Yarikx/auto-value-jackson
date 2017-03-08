import com.google.auto.value.extension.AutoValueExtension;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

public class AutoClass {
    final TypeElement typeElement;
    final List<Property> properties;

    AutoClass(TypeElement typeElement, List<Property> properties) {
        this.typeElement = typeElement;
        this.properties = properties;
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public List<Property> getProperties() {
        return properties;
    }


    public static AutoClass parse(AutoValueExtension.Context context) {
        List<Property> properties = new ArrayList<>();
        for (String key : context.properties().keySet()) {
            ExecutableElement executableElement = context.properties().get(key);
            Property property = Property.parse(key, executableElement);
            properties.add(property);
        }

        return new AutoClass(context.autoValueClass(), properties);
    }

    public Object[] allKeys() {
        String[] keys = new String[properties.size()];
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            keys[i] = property.getKey();
        }
        return keys;
    }
}
