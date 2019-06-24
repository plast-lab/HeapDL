package heapdl.hprof;

import edu.tufts.eaftan.hprofparser.handler.NullRecordHandler;
import edu.tufts.eaftan.hprofparser.parser.datastructures.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SnapshotHandler extends NullRecordHandler {

    private final Snapshot snapshot;
    private final HashMap<Long, String> stringMap = new HashMap<>();
    private final HashMap<Long, String> className = new HashMap<>();
    private final HashMap<Long, ClassInfo> classMap = new HashMap<>();

    private final HashMap<Integer, String> serialClassName = new HashMap<>();
    private final HashMap<Long, StackFrame> localStackFrames = new HashMap<>();
    private final HashMap<Integer, StackTrace> localStackTraces = new HashMap<>();

    private final StackTraces externalStackTraces;

    private final boolean extractStringConstants;

    public SnapshotHandler(Snapshot snapshot, StackTraces stackTraces, boolean extractStringConstants) {
        this.snapshot = snapshot;
        this.externalStackTraces = stackTraces;
        this.extractStringConstants = extractStringConstants;
    }

    @Override
    public void stringInUTF8(long id, String data) {
        // store string for later lookup
        stringMap.put(id, data);
    }

    @Override
    public void loadClass(int classSerialNum, long classObjId,
                          int stackTraceSerialNum, long classNameStringId) {
        className.put(classObjId, stringMap.get(classNameStringId).replaceAll("/", "."));
        serialClassName.put(classSerialNum, stringMap.get(classNameStringId).replaceAll("/", "."));
    }

    @Override
    public void stackFrame(long stackFrameId, long methodNameStringId,
                           long methodSigStringId, long sourceFileNameStringId,
                           int classSerialNum, int location) {
        localStackFrames.put(stackFrameId, new StackFrame(stringMap.get(methodNameStringId), stringMap.get(methodSigStringId), serialClassName.get(classSerialNum) ,location));
    }

    @Override
    public void stackTrace(int stackTraceSerialNum, int threadSerialNum,
                           int numFrames, long[] stackFrameIds) {
        List<StackFrame> sf = new ArrayList<>();
        for (long sfi: stackFrameIds) {
            sf.add(localStackFrames.get(sfi));
        }
        if(sf.size() > 0) {
            localStackTraces.put(stackTraceSerialNum, new StackTrace(sf.toArray(new StackFrame[0])));
        }
    }

    @Override
    public void classDump(long classObjId, int stackTraceSerialNum,
                          long superClassObjId, long classLoaderObjId, long signersObjId,
                          long protectionDomainObjId, long reserved1, long reserved2,
                          int instanceSize, Constant[] constants, Static[] statics,
                          InstanceField[] instanceFields) {
        List<JavaField> javaFields = new ArrayList<>();
        for (Static s : statics) {
            javaFields.add(new JavaField(stringMap.get(s.staticFieldNameStringId), s.value.type.toString(), s.value.value.toString(), className.get(classObjId)));
        }

        snapshot.addThing(classObjId, new JavaClass(classObjId, className.get(classObjId), javaFields, getStackTraces(stackTraceSerialNum, classObjId)));

        classMap.put(classObjId, new ClassInfo(classObjId, superClassObjId, instanceSize, instanceFields));
    }

    @Override
    public void instanceDump(long objId, int stackTraceSerialNum,
                             long classObjId, Value<?>[] instanceFieldValues) {

        List<JavaField> javaFields = new ArrayList<>();

        if (instanceFieldValues.length > 0) {
            // superclass of Object is 0
            int i = 0;
            long nextClass = classObjId;
            while (nextClass != 0) {
                ClassInfo ci = classMap.get(nextClass);
                nextClass = ci.superClassObjId;
                for (InstanceField field : ci.instanceFields) {
                    javaFields.add(new JavaField(stringMap.get(field.fieldNameStringId), field.type.toString(), instanceFieldValues[i].value.toString(), className.get(ci.classObjId)));
                    i++;
                }
            }
            assert i == instanceFieldValues.length;
        }
        snapshot.addThing(objId, new JavaObject(objId, classObjId, className.get(classObjId), javaFields, getStackTraces(stackTraceSerialNum, objId)));
    }

    @Override
    public void objArrayDump(long objId, int stackTraceSerialNum,
                             long elemClassObjId, long[] elems) {
        snapshot.addThing(objId, new JavaObjectArray(objId, elemClassObjId, className.get(elemClassObjId), elems, getStackTraces(stackTraceSerialNum, objId)));
    }

    @Override
    public void primArrayDump(long objId, int stackTraceSerialNum,
                              byte elemType, Value<?>[] elems) {
        if (extractStringConstants && getBasicType(elemType).equals("byte") && elems.length < 50) {
            Byte[] elements = new Byte[elems.length];
            for (int i = 0; i < elems.length; i++) {
                elements[i] = (Byte) elems[i].value;
            }
            snapshot.addThing(objId, new JavaValueArray(objId, objId, getBasicTypeArray(elemType), elements, getStackTraces(stackTraceSerialNum, objId)));
        } else {
            snapshot.addThing(objId, new JavaValueArray(objId, objId, getBasicTypeArray(elemType), null, getStackTraces(stackTraceSerialNum, objId)));
        }
    }

    private StackTrace getStackTraces(int stackTraceSerialNum, long objId) {
        if (localStackTraces.containsKey(stackTraceSerialNum)) {
            return localStackTraces.get(stackTraceSerialNum);
        }
        return externalStackTraces.getStackTrace(objId);
    }

    private static String getBasicType(byte type) {
        switch (type) {
            case 2:
                return "object";
            case 4:
                return "boolean";
            case 5:
                return "char";
            case 6:
                return "float";
            case 7:
                return "double";
            case 8:
                return "byte";
            case 9:
                return "short";
            case 10:
                return "int";
            case 11:
                return "long";
            default:
                return null;
        }
    }

    //is this correct?
    private static String getBasicTypeArray(byte type) {
        switch (type) {
            case 2:
                return "[O";
            case 4:
                return "[Z";
            case 5:
                return "[C";
            case 6:
                return "[F";
            case 7:
                return "[D";
            case 8:
                return "[B";
            case 9:
                return "[S";
            case 10:
                return "[I";
            case 11:
                return "[J";
            default:
                return null;
        }
    }
}
