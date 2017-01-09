#! /bin/bash
set -e
./test_everything.sh target/Compiler-jar-with-dependencies.jar
./test_everything.sh target/Interpreter-jar-with-dependencies.jar
./test_domino.sh target/Compiler-jar-with-dependencies.jar
