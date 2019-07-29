package heapdl.hprof;

import heapdl.core.DumpParsingUtil;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.clyze.utils.TypeUtils;

public class StackTraces {

    private final HashMap<Long, StackTrace> stackTraces = new HashMap<>();

    public void addStackTraces(String filename) {
        String line;
        String objId = "1";
        List<StackFrame> stackFrames = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            while ((line = br.readLine()) != null) {
                if (line.contains("(null)"))
                    continue;

                String[] frame = line.split("\t");

                if (!objId.equals(frame[0])) {
                    stackTraces.put(Long.parseLong(objId), new StackTrace(stackFrames.toArray(new StackFrame[0])));

                    stackFrames = new ArrayList<>();
                    objId = frame[0];
                }
                String methodSignature = frame[3];
                //System.out.println("Method signature: " + frame[3]);
                String arguments = methodSignature.substring(methodSignature.indexOf("(")+1, methodSignature.lastIndexOf(")"));
	            String raisedReturnType = TypeUtils.raiseTypeId(methodSignature.substring(methodSignature.lastIndexOf(")")+1));
	            String raisedSignature = raisedReturnType + " " + frame[2] + "(";

	            if (!arguments.isEmpty())
                    raisedSignature += DumpParsingUtil.convertArguments(arguments);
	            raisedSignature += ")";
                stackFrames.add(new StackFrame(frame[2], raisedSignature, TypeUtils.raiseTypeId(frame[5]), Integer.parseInt(frame[4])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StackTrace getStackTrace(long objId) {
        return stackTraces.get(objId);
    }

    public Collection<StackTrace> getAllStackTraces() {
        return stackTraces.values();
    }
}
