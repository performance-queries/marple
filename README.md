[![Build Status](https://travis-ci.org/anirudhSK/needlstk.svg?branch=master)](https://travis-ci.org/anirudhSK/needlstk)

needlstk: A system for expressing network performance queries

The name is a portmanteau of needle and stack to reflect the
goal of finding needles in a haystack. It is pronounced "needle stack"

QuickStart (requires maven)

0. mvn package
Packages sources into one assembly JAR for
the compiler and the interpreter

1. ./smoke\_test.sh
Runs smoke tests.
These tests don't mean too much if they pass,
but if you get an exception for any one, it
probably means there's a bug.

2. Generate parse tree using
java -cp /usr/local/lib/antlr-4.5.3-complete.jar:target/classes/ org.antlr.v4.gui.TestRig edu.mit.needlstk.PerfQuery prog example\_queries/flowlet\_hist.sql -gui

(replace /usr/local/lib/antlr-4.5.3-complete.jar with wherever it's installed in your system)
(should open up a window showing the parse tree)

3. Run compiler using
cat example\_queries/flowlet\_hist.sql | java -ea -jar target/Compiler-jar-with-dependencies.jar

4. Run interpreter using
cat example\_queries/flowlet\_hist.sql | java -ea -jar target/Interpreter-jar-with-dependencies.jar 2> /tmp/output.py

5. Run interpreted code using
python3 /tmp/output.py
