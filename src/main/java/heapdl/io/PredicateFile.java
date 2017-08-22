package heapdl.io;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public enum PredicateFile {
    DYNAMIC_NORMAL_HEAP_OBJECT("DynamicNormalHeapObject"),
    DYNAMIC_STRING_HEAP_OBJECT("DynamicStringHeapObject"),
    DYNAMIC_STATIC_FIELD_POINTS_TO("DynamicStaticFieldPointsTo"),
    DYNAMIC_INSTANCE_FIELD_POINTS_TO("DynamicInstanceFieldPointsTo"),
    DYNAMIC_ARRAY_INDEX_POINTS_TO("DynamicArrayIndexPointsTo"),
    DYNAMIC_VAR_POINTS_TO("DynamicVarPointsTo"),
    DYNAMIC_REACHABLE_METHOD("DynamicReachableMethod"),
    DYNAMIC_CALL_GRAPH_EDGE("DynamicCallGraphEdge"),
    DYNAMIC_NORMAL_HEAP_ALLOCATION("DynamicNormalHeapAllocation"),
    DYNAMIC_CONTEXT("DynamicContext"),
    STRING_CONST("StringConstant"),
    STRING_RAW("StringRaw");

    private final String name;

    PredicateFile(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return  name;
    }

    public Writer getWriter(File directory, String suffix) throws IOException {
        File factsFile = new File(directory, name + suffix);
        FileUtils.touch(factsFile);
        return new FileWriter(factsFile, true);
    }
}
