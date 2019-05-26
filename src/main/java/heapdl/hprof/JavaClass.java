package heapdl.hprof;

import java.util.List;

public class JavaClass extends JavaThing {

    private final List<JavaField> statics;

    public JavaClass(long id, String name, List<JavaField> statics, StackTrace stackTrace) {
        super(id, id, name, stackTrace);
        this.statics = statics;
    }

    public List<JavaField> getStatics() {
        return statics;
    }
}
