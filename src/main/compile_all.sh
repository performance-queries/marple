#!/bin/bash

### This tests the compilation pipeline of all queries in $QUERY_DIR.
### If it encounters an error, it prints that error and asks if you want to proceed.
### The compiled JSON is *not* removed after this script finishes.

go build
numfiles=$(ls ../../example_queries | wc -l)
i=1
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
    fragsf=${f/.sql/.frags}
    mv p4-frags.txt $fragsf
    p4f=${f/.sql/.p4}
    cat $fragsf | ./main > $p4f 2> /tmp/agerr
    status=$?
    if [ $status -ne 0 ]
    then
        echo
        echo "Autogenerating $p4f failed with the following error:"
        cat /tmp/agerr
        exit 1
    fi
    jsonf=${f/.sql/.json}
    ~/p4c/build/p4c-bm2-ss $p4f -o $jsonf > /dev/null 2> /tmp/p4err
    status=$?
    if [ $status -ne 0 ]
    then
        echo
        echo "P4 Compiler failed on $p4f with the following error:"
        cat /tmp/p4err
        #exit 1
        read -p "Got error. Press ENTER to continue" "input"
    fi
    i=$((i+1))
done
echo -e "\r\033[KCompilation complete!"
exit 1
rm *.p4
rm *.frags
rm domino-full.c
