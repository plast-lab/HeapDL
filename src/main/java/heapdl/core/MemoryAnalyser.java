package heapdl.core;

import com.sun.tools.hat.internal.model.*;
import heapdl.io.Database;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;



/**
 * Created by neville on 19/01/2017.
 */
public class MemoryAnalyser {

    static boolean EXTRACT_STRING_CONSTANTS = false;

    private static String filename;

    private Set<DynamicFact> dynamicFacts = ConcurrentHashMap.newKeySet();

    private HeapAbstractionIndexer heapAbstractionIndexer = null;

    public MemoryAnalyser(String filename, boolean uniqueStings) {

        this.filename = filename;
        EXTRACT_STRING_CONSTANTS = uniqueStings;
        if (uniqueStings) System.out.println("(Experimental) Strings in Heap dump will be analyzed.");
    }



    public void resolveFactsFromDump(String sensitivity) throws IOException, InterruptedException {
        Snapshot snapshot = DumpParsingUtil.getSnapshotFromFile(filename);


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


        System.out.println("Extracting facts from heap dump...");

        Set<DynamicInstanceFieldPointsTo> dynamicInstanceFieldPointsToSet = ConcurrentHashMap.newKeySet();
        Set<DynamicArrayIndexPointsTo> dynamicArrayIndexPointsToSet = ConcurrentHashMap.newKeySet();
        Set<DynamicStaticFieldPointsTo> dynamicStaticFieldPointsToSet = ConcurrentHashMap.newKeySet();

        Enumeration<JavaHeapObject> instances = snapshot.getThings();
        Collections.list(instances).parallelStream().forEach(heap -> {
            if (heap instanceof JavaObject) {
                JavaObject obj = (JavaObject) heap;
                String baseHeap = heapAbstractionIndexer.getAllocationAbstraction(obj);
                JavaClass clazz = obj.getClazz();
                if (obj.getClazz().toString().startsWith("heapdl") ||
                        obj.getClazz().toString().startsWith("javassist")) return;
                do {
                    for (JavaField field : clazz.getFields()) {
                        JavaThing fieldValue = obj.getField(field.getName());
                        dynamicInstanceFieldPointsToSet.add(new DynamicInstanceFieldPointsTo(baseHeap, field.getName(), clazz.getName(), heapAbstractionIndexer.getAllocationAbstraction(fieldValue)));
                    }
                } while ((clazz = clazz.getSuperclass()) != null);
            } else if (heap instanceof  JavaObjectArray) {
                JavaObjectArray obj = (JavaObjectArray) heap;
                String baseHeap = heapAbstractionIndexer.getAllocationAbstraction(obj);
                for (JavaThing value : obj.getElements()) {
                    if (value != null)
                        dynamicArrayIndexPointsToSet.add(new DynamicArrayIndexPointsTo(baseHeap, heapAbstractionIndexer.getAllocationAbstraction(value)));
                }
            } else if (heap instanceof  JavaValueArray) {
                // Nothing to do here
            } else if (heap instanceof JavaClass) {
                JavaClass obj = (JavaClass) heap;
                for (JavaStatic javaStatic : obj.getStatics()) {
                    dynamicStaticFieldPointsToSet.add(new DynamicStaticFieldPointsTo(
                            javaStatic.getField().getName(), obj.getName(),
                            heapAbstractionIndexer.getAllocationAbstraction(javaStatic.getValue())
                    ));
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

        try {
            long startTime = System.nanoTime();
            resolveFactsFromDump(sensitivity);
            long endTime = System.nanoTime();
            long durationSeconds = (endTime - startTime) / 1000000000;
            System.out.println("Heap dump analysis time: " + durationSeconds);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        Context.write_facts_once(db);

        for (DynamicFact fact: dynamicFacts) {
            fact.write_fact(db);
        }

        for (DynamicFact fact: heapAbstractionIndexer.getDynamicFacts()) {
            fact.write_fact(db);
        }

        db.flush();
        db.close();
        return dynamicFacts.size();
    }




}
