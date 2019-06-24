package heapdl.core;

import edu.tufts.eaftan.hprofparser.parser.HprofParser;
import heapdl.hprof.*;
import heapdl.io.Database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by neville on 19/01/2017.
 */
public class MemoryAnalyser {

    static boolean EXTRACT_STRING_CONSTANTS = false;

    private final List<String> filenames;

    private final List<String> stackTracesFilenames;

    private Set<DynamicFact> dynamicFacts = ConcurrentHashMap.newKeySet();

    private HeapAbstractionIndexer heapAbstractionIndexer = null;

    private StackTraces stackTraces;

    public MemoryAnalyser(List<String> filenames, List<String> stackTracesFilenames, boolean uniqueStings) {
        this.filenames = filenames;
        EXTRACT_STRING_CONSTANTS = uniqueStings;
        if (uniqueStings) System.out.println("(Experimental) Strings in Heap dump will be analyzed.");
        this.stackTracesFilenames = stackTracesFilenames;
    }

    public void resolveFactsFromDump(String filename, String sensitivity) throws IOException, InterruptedException {
        Snapshot snapshot = new Snapshot();
        HprofParser hprofParser = new HprofParser(new SnapshotHandler(snapshot, stackTraces, EXTRACT_STRING_CONSTANTS));
        hprofParser.parse(new File(filename));

        /*
        try {
            Class<?> heapAbstractionIndexerClass = Class.forName(
                    getClass().getPackage().getName()+".HeapAbstractionIndexer" + sensitivity
            );
            heapAbstractionIndexer = (HeapAbstractionIndexer) heapAbstractionIndexerClass
                    .getConstructor(Snapshot.class)
                    .newInstance(snapshot);
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException |
                ClassNotFoundException | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        */
        heapAbstractionIndexer = new HeapAbstractionIndexerInsensitive(snapshot);


        System.out.println("Extracting facts from heap dump...");

        Set<DynamicInstanceFieldPointsTo> dynamicInstanceFieldPointsToSet = ConcurrentHashMap.newKeySet();
        Set<DynamicArrayIndexPointsTo> dynamicArrayIndexPointsToSet = ConcurrentHashMap.newKeySet();
        Set<DynamicStaticFieldPointsTo> dynamicStaticFieldPointsToSet = ConcurrentHashMap.newKeySet();

        ArrayList<JavaThing> instances = snapshot.getThings();
        instances.parallelStream().forEach(heap -> {
            if (heap instanceof JavaObject) {
                JavaObject obj = (JavaObject) heap;
                String baseHeap = heapAbstractionIndexer.getAllocationAbstraction(obj);

                String objCls = obj.getClassName();
                if (objCls.startsWith("heapdlpp") ||
                        objCls.startsWith("javassist") ||
                        objCls.startsWith("java.lang.String")) return;
                for (JavaField field : obj.getFields()) {
                    if (field.getType().equals("Object")) {
                        JavaThing fieldObj = snapshot.getObj(Long.parseLong(field.getValue()));
                        if (fieldObj != null) {
                            dynamicInstanceFieldPointsToSet.add(new DynamicInstanceFieldPointsTo(baseHeap, field.getName(), field.getOwnerClass(), heapAbstractionIndexer.getAllocationAbstraction(fieldObj)));
                        } else {
                            dynamicInstanceFieldPointsToSet.add(new DynamicInstanceFieldPointsTo(baseHeap, field.getName(), field.getOwnerClass(), "Primitive Object"));
                        }
                    } else {
                        dynamicInstanceFieldPointsToSet.add(new DynamicInstanceFieldPointsTo(baseHeap, field.getName(), field.getOwnerClass(), "Primitive Object"));
                    }
                }
            } else if (heap instanceof JavaObjectArray) {
                JavaObjectArray obj = (JavaObjectArray) heap;
                String baseHeap = heapAbstractionIndexer.getAllocationAbstraction(obj);
                for (long value : obj.getElements()) {
                    JavaThing fieldObj = snapshot.getObj(value);
                    if (fieldObj != null) {
                        dynamicArrayIndexPointsToSet.add(new DynamicArrayIndexPointsTo(baseHeap, heapAbstractionIndexer.getAllocationAbstraction(fieldObj)));
                    }
                }
            } else if (heap instanceof JavaValueArray) {
                // Nothing to do here
            } else if (heap instanceof JavaClass) {
                JavaClass obj = (JavaClass) heap;
                for (JavaField javaField : obj.getStatics()) {
                    if (javaField.getType().equals("Object")) {
                        JavaThing fieldObj = snapshot.getObj(Long.parseLong(javaField.getValue()));
                        if (fieldObj != null) {
                            dynamicStaticFieldPointsToSet.add(new DynamicStaticFieldPointsTo(javaField.getName(), obj.getClassName(), heapAbstractionIndexer.getAllocationAbstraction(fieldObj)));
                        } else {
                            dynamicStaticFieldPointsToSet.add(new DynamicStaticFieldPointsTo(javaField.getName(), obj.getClassName(), "Primitive Object"));
                        }
                    } else {
                        dynamicStaticFieldPointsToSet.add(new DynamicStaticFieldPointsTo(javaField.getName(), obj.getClassName(), "Primitive Object"));
                    }
                }
            } else {
                throw new RuntimeException("Unknown: " + heap.getClass().toString());
            }
        });

        dynamicFacts.addAll(dynamicStaticFieldPointsToSet);
        dynamicFacts.addAll(dynamicInstanceFieldPointsToSet);
        dynamicFacts.addAll(dynamicArrayIndexPointsToSet);
    }

    public int getAndOutputFactsToDB(File factDir, String sensitivity) throws IOException, InterruptedException {
        Database db = new Database(factDir, false);

        stackTraces = new StackTraces();
        for (String filename : stackTracesFilenames) {
            stackTraces.addStackTraces(filename);
        }

        for (String filename : filenames) {
            try {
                long startTime = System.nanoTime();
                resolveFactsFromDump(filename, sensitivity);
                long endTime = System.nanoTime();
                long durationSeconds = (endTime - startTime) / 1000000000;
                System.out.println("Heap dump analysis time: " + durationSeconds);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }

            Context.write_facts_once(db);

            for (DynamicFact fact : dynamicFacts) {
                fact.write_fact(db);
            }

            for (DynamicFact fact : heapAbstractionIndexer.getDynamicFacts()) {
                fact.write_fact(db);
            }
        }

        db.flush();
        db.close();
        return dynamicFacts.size();
    }

}
