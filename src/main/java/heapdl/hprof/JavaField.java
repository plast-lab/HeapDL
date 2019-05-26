package heapdl.hprof;

public class JavaField {
    private final String name;
    private final String type;
    private final String value;
    private final String ownerClass;

    public JavaField(String name, String type, String value, String ownerClass) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.ownerClass = ownerClass;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getOwnerClass() {
        return ownerClass;
    }
}
