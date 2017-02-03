#!/bin/bash

### This tests the compilation pipeline of all queries in $QUERY_DIR.
### If it encounters an error, it prints that error and asks if you want to proceed.
### The compiled JSON is *not* removed after this script finishes.

QUERY_DIR=../../example_queries
numfiles=$(ls $QUERY_DIR | wc -l)
i=1
mkdir -p outputs/p4
mkdir -p outputs/json
for f in `ls ../../example_queries`
do
    echo -ne "\r\033[K[Compiling $f ($i of $numfiles)]"
    ff=../../example_queries/$f
    cat $ff | java -ea -jar ../../target/Compiler-jar-with-dependencies.jar > /dev/null 2> /tmp/javacerr
    status=$?
    if [ $status -ne 0 ]
    then
        echo
        echo "Java compiler failed on $f with the following error:"
        cat /tmp/javacerr
        exit 1
    fi
    jsonf=outputs/json/${f/.sql/.json}
    ~/p4c/build/p4c-bm2-ss output.p4 -o $jsonf > /dev/null 2> /tmp/p4err
    rm -rf output.p4
    status=$?
    if [ $status -ne 0 ]
    then
        echo
        echo "P4 Compiler failed on output.p4 with the following error:"
        cat /tmp/p4err
        exit 1
        #read -p "Got error. Press ENTER to continue" "input"
    fi
    i=$((i+1))
done
echo -e "\r\033[KCompilation complete!"
rm -f domino-full.c
