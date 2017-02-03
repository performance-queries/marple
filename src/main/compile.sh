#!/bin/bash

### This tests the compilation pipeline of all queries in $QUERY_DIR.
### If it encounters an error, it prints that error and asks if you want to proceed.
### The compiled JSON is *not* removed after this script finishes.

mkdir -p outputs/p4
mkdir -p outputs/json
ff=$1
f=$(echo $ff | sed -e 's/.*\/\(.*\.sql\)/\1/')
cat $ff | java -ea -jar ~/marple/target/Compiler-jar-with-dependencies.jar > /dev/null 2> /tmp/javacerr
status=$?
if [ $status -ne 0 ]
then
    echo
    echo "Java compiler failed on $f with the following error:"
    cat /tmp/javacerr
    exit 1
fi
jsonf=outputs/json/${f/.sql/.json}
mv output.p4 outputs/p4/${f/.sql/.p4}
~/p4c/build/p4c-bm2-ss outputs/p4/${f/.sql/.p4} -o $jsonf > /dev/null 2> /tmp/p4err
status=$?
if [ $status -ne 0 ]
then
    echo
    echo "P4 Compiler failed on $p4f with the following error:"
    cat /tmp/p4err
    exit 1
fi
echo "Compilation complete."
rm -f domino-full.c
