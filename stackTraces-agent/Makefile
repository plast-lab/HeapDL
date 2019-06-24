CC?=gcc
JNI_INCLUDE_PATH?=$(JAVA_HOME)/include
EXTRA_INCLUDE_PATH?=$(JAVA_HOME)/include/linux

all:
	$(CC) -Wall -fPIC -I"$(JNI_INCLUDE_PATH)" -I"$(EXTRA_INCLUDE_PATH)" -shared -o libStackTracesAgent.so libStackTracesAgent.c

clean:
	rm -rf libStackTracesAgent.so
