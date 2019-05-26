package heapdl.hprof;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StackTraces {

    private final HashMap<Long, StackTrace> stackTraces = new HashMap<>();

    public void addStackTraces(String filename) {
        String line;
        String objId = "1";
        List<StackFrame> stackFrames = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            while ((line = br.readLine()) != null) {
                String[] frame = line.split("\t");
                if (!objId.equals(frame[0])) {
                    stackTraces.put(Long.parseLong(objId), new StackTrace(stackFrames.toArray(new StackFrame[0])));

                    stackFrames = new ArrayList<>();
                    objId = frame[0];
                }
                stackFrames.add(new StackFrame(frame[2], frame[3], frame[5], Integer.parseInt(frame[4])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StackTrace getStackTrace(long objId) {
        return stackTraces.get(objId);
    }
}
