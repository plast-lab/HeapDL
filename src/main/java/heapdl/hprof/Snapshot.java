package heapdl.hprof;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Snapshot {

    private final Map<Long, JavaThing> javaThings = new HashMap<>();

    void addThing(long id, JavaThing javaThing) {
        javaThings.put(id, javaThing);
    }

    public ArrayList<JavaThing> getThings() {
        return new ArrayList<>(javaThings.values());
    }

    public JavaThing getObj(long id) {
        return javaThings.get(id);
    }
}
