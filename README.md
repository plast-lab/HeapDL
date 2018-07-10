# HeapDL
Heaps Don't Lie!  Published in PACM PL OOPSLA (2017) ([pdf](http://www.nevillegrech.com/heapdl-oopsla17.pdf))

# Using HeapDL in standalone mode

```
$ ./gradlew fatjar
$ java -jar build/libs/HeapDL-all-1.0.3.jar file.hprof --out output-dir
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

## OpenJDK/IBM JDK

To take a heap snapshot of a running program (`Program.jar`) on
program exit, run:

```
java -agentlib:hprof=heap=dump,format=b,depth=8 -jar Program.jar
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
