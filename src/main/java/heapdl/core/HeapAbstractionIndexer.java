package heapdl.core;


import heapdl.hprof.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static heapdl.core.DumpParsingUtil.fullyQualifiedMethodSignatureFromFrame;

/**
 * Created by neville on 15/03/2017.
 */
class HeapAbstractionIndexer {
    private static final String UNKNOWN = "Unknown";
    private final Map<Long, DynamicHeapObject> heapIndex = new ConcurrentHashMap<>();
    private final Map<AllocationKey, DynamicHeapObject> frameIndex = new ConcurrentHashMap<>();
    private final Snapshot snapshot;

    private final Set<DynamicFact> dynamicFacts = ConcurrentHashMap.newKeySet();

    HeapAbstractionIndexer(Snapshot snapshot) {
        this.snapshot = snapshot;
    }

    protected StackFrame getAllocationFrame(JavaThing obj) {
        StackTrace trace = obj.getStackTrace();
        // Store dynamic edges
        //dynamicFacts.addAll(DynamicCallGraphEdge.fromStackTrace(trace));
        //dynamicFacts.addAll(DynamicReachableMethod.fromStackTrace(trace));

        //JavaClass clazz = obj.getClazz();
        String clazz = obj.getClassName();
        if (trace != null && trace.getFrames().length > 0) {
            StackFrame[] frames = trace.getFrames();

            int relevantIndex = 0;
            // find index where object is allocated
            if (clazz.startsWith("[L") || clazz.equals("java.lang.Object") || !frames[0].getMethodName().equals("<init>")) {
                relevantIndex = 0;
            } else {
                for (int i = 0; i < frames.length - 1; i++) {
                    if (frames[i].getClassName().equals(clazz) && frames[i].getMethodName().equals("<init>"))
                        relevantIndex = i + 1;
                    else if (relevantIndex > 0) break;
                }
            }

            return frames[relevantIndex];
        }
        return null;

    }

    DynamicHeapObject getHeapRepresentation(JavaThing obj, Context hctx) {
        // always get the frame, even if unused, due to side-effects of 'getAllocationFrame'
        StackFrame frame = getAllocationFrame(obj);

        if (MemoryAnalyser.EXTRACT_STRING_CONSTANTS && obj.getClassName().equals("java.lang.String")) { //&& obj.toString().length() < 50) {
            String strValue = UNKNOWN;

            JavaField field = ((JavaObject) obj).getField("value");
            if (field != null) {
                JavaValueArray strObj = (JavaValueArray) snapshot.getObj(Long.parseLong(field.getValue()));
                if (strObj != null && strObj.getLength() > 0 && strObj.getLength() < 50) {
                    strValue = toHex(strObj.getElements());
                }
            }
            return new DynamicStringHeapObject(strValue);
        }

        Function<AllocationKey, DynamicHeapObject> heapObjectFromFrame = k -> {
            StackFrame f = k.getFrame();

            String fullyQualifiedMethodName = fullyQualifiedMethodSignatureFromFrame(f);

            return new DynamicNormalHeapObject(DumpParsingUtil.parseLineNumber(f.getLineNumber()), fullyQualifiedMethodName, obj.getClassName(), hctx.getRepresentation());
        };

        final AllocationKey allocationKey = new AllocationKey(frame, obj.getClassId());
        // This is the point to perform caching

        if (hctx.equals(ContextInsensitive.get()) && frame != null) {
            return frameIndex.computeIfAbsent(allocationKey, heapObjectFromFrame);
        }


        if (frame == null)
            return new DynamicNormalHeapObject(DumpParsingUtil.UNKNOWN_LINE, UNKNOWN, obj.getClassName(), hctx.getRepresentation());

        return heapObjectFromFrame.apply(allocationKey);
    }

    private String toHex(Byte[] value) {
        StringBuilder result = new StringBuilder("{");

        boolean first = true;
        for (Byte b : value) {
            if (first) {
                result.append("0x").append(String.format("%x", b));
                first = false;
            } else {
                result.append(", 0x").append(String.format("%x", b));
            }
        }
        result.append("}");
        return result.toString();
    }

    Set<DynamicFact> getDynamicFacts() {
        return dynamicFacts;
    }


    // public facade with caching
    String getAllocationAbstraction(JavaThing obj) {
        if (obj instanceof JavaValueArray ||
                obj instanceof JavaObjectArray ||
                obj instanceof JavaObject) {
            DynamicHeapObject heapAbstraction = heapIndex.getOrDefault(obj.getId(), null);
            if (heapAbstraction != null) return heapAbstraction.getRepresentation();

            heapAbstraction = getHeapRepresentation(obj, ContextInsensitive.get());

            addFact(heapAbstraction);

            return heapAbstraction.getRepresentation();

//        } else if (obj instanceof JavaValue) {
//            return "Primitive Object";
//        } else if (obj instanceof JavaObjectRef) {
//            return "JavaObjectRef";
        } else if (obj instanceof JavaClass) {
            DynamicClassHeapObject cls = new DynamicClassHeapObject((JavaClass) obj);
            addFact(cls);
            return cls.getRepresentation();
        }
        throw new RuntimeException("Unknown: " + obj.getClass().toString());
    }

    void addFact(DynamicFact fact) {
        dynamicFacts.add(fact);
    }
}
