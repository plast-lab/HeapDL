package heapdl.io;

import com.sun.tools.hat.internal.model.JavaClass;

/**
 * Created by neville on 15/02/2017.
 */
public class FactEncoders {
    public static String encodeStringConstant(String constant) {
        String raw;
        if(constant.trim().equals(constant) && constant.length() > 0)
            raw = constant;
        else
            raw = "<<\"" + constant + "\">>";
        return raw;
    }

    public static String encodeClass(JavaClass obj) {
        return "<class " + obj.getName() + ">";
    }
}
