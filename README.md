# HeapDL
Heaps Don't Lie!  Published in PACM PL OOPSLA (2017) ([pdf](http://www.nevillegrech.com/heapdl-oopsla17.pdf))

HeapDL is integrated with the Doop pointer analysis framework, however it is also available as a standalone tool.

# Using HeapDL in standalone mode

```
$ ./gradlew fatjar
$ java -jar build/libs/HeapDL-all-1.1.6.jar file.hprof --out output-dir
```

# Using HeapDL as a library
````
repositories {
        jcenter()
        maven { url "https://jitpack.io" }
   }
   dependencies {
         compile 'com.github.plast-lab:HeapDL:master-SNAPSHOT'
   }
````

# Generating a heap snapshot for use with HeapDL

## OpenJDK/IBM JDK (Java <= 8)

To take a heap snapshot of a running program (`Program.jar`) on
program exit, run:

```
java -agentlib:hprof=heap=dump,format=b,depth=8 -jar Program.jar
```

It will produce a java.hprof file and then you can run HeapDL
with:

```
java -jar build/libs/HeapDL-all-1.1.1.jar java.hprof --out output-dir
```

## OpenJDK (Java 9-10)

To take a heap snapshot of a program (`Program.jar`) on
program exit, first build the [heapDump-agent](heapDump-agent/README.md) and
then run:

```
java -javaagent:heapDump-agent/target/theLastDump-1.0-SNAPSHOT-jar-with-dependencies.jar -jar Program.jar
```

The heap snapshot for Java 9-10 doesn't contain stack
traces. It will only produce a heap-dump.hprof and then you
can run HeapDL with:

```
java -jar build/libs/HeapDL-all-1.1.1.jar heap-dump.hprof --out output-dir
```

## OpenJDK (Java >= 11)

To take a heap snapshot of a program (`Program.jar`) on
program exit, first build the [heapDump-agent](heapDump-agent/README.md) and the
[stackTraces-agent](stackTraces-agent/README.md) and then run:

```
java -javaagent:heapDump-agent/target/theLastDump-1.0-SNAPSHOT-jar-with-dependencies.jar -agentpath:stackTraces-agent/libStackTracesAgent.so=sampling=512kb,depth=8 -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -jar Program.jar
```

For the stackTraces-agent you can configure the
sampling rate and the depth of the stack traces
by using the optional arguments.

The above command will produce a heap-dump.hprof
file and a stackTraces.csv and then you can run
HeapDL with:

```
java -jar build/libs/HeapDL-all-1.1.1.jar heap-dump.hprof --stackTraces stackTraces.csv --out output-dir
```

## Android

To take a heap snapshot an Android app running on a device (or
emulator) bridged by `adb`, run the following
(`APP_PACKAGE`/`MAIN_ACTIVITY` are the names of the application
package and the main app activity to launch respectively):

```
# Start app.
adb shell am start --track-allocation $APP_PACKAGE/$MAIN_ACTIVITY

# When ready, take heap snapshot (we assume $2 is the PID field of ps).
adb shell am dumpheap `adb shell ps | grep $APP_PACKAGE\$ | awk '{print $2}'` /data/local/tmp/$APP_PACKAGE.android.hprof

# Download and convert.
adb pull /data/local/tmp/$APP_PACKAGE.android.hprof .
hprof-conv $APP_PACKAGE.android.hprof $APP_PACKAGE.hprof
```
