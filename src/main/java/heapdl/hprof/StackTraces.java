package heapdl.hprof;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static heapdl.core.DumpParsingUtil.*;

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
                String methodSignature = frame[3];
                //System.out.println("Method signature: " + frame[3]);
                String arguments = methodSignature.substring(methodSignature.indexOf("(")+1, methodSignature.lastIndexOf(")"));
	            String raisedReturnType = raiseTypeId(methodSignature.substring(methodSignature.lastIndexOf(")")+1));
	            String raisedSignature = raisedReturnType + " " + frame[2] + "(";

	            if (!arguments.isEmpty()) {
                    raisedSignature += convertArguments(arguments);
	            }
	            raisedSignature += ")";
                stackFrames.add(new StackFrame(frame[2], raisedSignature, raiseTypeId(frame[5]), Integer.parseInt(frame[4])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StackTrace getStackTrace(long objId) {
        return stackTraces.get(objId);
    }
}
