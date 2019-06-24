package heapdl.hprof;

import java.util.List;

public class JavaObject extends JavaThing {
    private final List<JavaField> fields;

    public JavaObject(long id, long classId, String className, List<JavaField> fields, StackTrace stackTrace) {
        super(id, classId, className, stackTrace);
        this.fields = fields;
    }

    public List<JavaField> getFields() {
        return fields;
    }

    public JavaField getField(String fieldName) {
        for (JavaField field : fields) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }
}
