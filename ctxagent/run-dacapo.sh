#!/bin/bash

# Runs the dacapo-bach benchmarks with the context agent. Notes:
#
# - tradebeans needs a specific free local port to work
#
# - eclipse needs a special agent to run under modern JVMs
#   (https://github.com/jon-bell/dacapo-eclipse-hacker)
#

JAVA=java

AGENT=${HOME}/doop-nexgen/HeapDL/ctxagent/build/libs/ctxagent-1.0-SNAPSHOT.jar
CG=cg
# CG=

function bench {
    echo "DaCapo benchmark: ${1}"
    time ${JAVA} -javaagent:${AGENT}=${CG} -Xss10000m -cp ${AGENT}:${HOME}/doop-benchmarks/dacapo-bach/dacapo-9.12-bach.jar Harness ${1} -s default -t 1
}

if [ -z "$1" ]
then
    for b in avrora batik h2 jython luindex lusearch pmd sunflow xalan
    # for b in eclipse tradebeans
    do
        bench ${b}
    done
else
    bench ${1}
fi
