all: jni java

jni:
	gcc -fPIC -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/linux" -shared -o libhello.so HelloJNI.c

java:
	javac HelloJNI.java
