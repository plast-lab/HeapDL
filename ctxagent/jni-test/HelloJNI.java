// Modified version of:
// https://www3.ntu.edu.sg/home/ehchua/programming/java/JavaNativeInterface.html
public class HelloJNI {
   static {
      System.loadLibrary("hello"); // Load native library at runtime
                                   // hello.dll (Windows) or libhello.so (Unixes)
   }
 
   // Declare a native method sayHello() that receives nothing and returns void
   private native void sayHello();
   private native Object newJNIObj();
 
   // Test Driver
   public static void main(String[] args) {
      HelloJNI hj = new HelloJNI();
      hj.sayHello();  // invoke the native method
      Object obj = hj.newJNIObj();
      System.out.println(obj.toString());
   }
}
