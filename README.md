[![Build Status](https://travis-ci.org/anirudhSK/needlstk.svg?branch=master)](https://travis-ci.org/anirudhSK/needlstk)

needlstk: A system for expressing network performance queries

The name is a portmanteau of needle and stack to reflect the
goal of finding needles in a haystack. It is pronounced "needle stack"

QuickStart:

0. ./smoke\_test.sh

Builds java and ANTLR sources and runs smoke tests.
These tests don't mean too much if they pass,
but if you get an exception for any one, it
probably means there's a bug.

1. . setup.sh (Don't execute setup.sh, source it)

Sets up aliases and env variables so that the Compiler
and Interpreter can be used interactively.

2. Generate parse tree using
grun PerfQuery prog  example\_queries/flowlet\_hist.sql -gui

(should open up a window showing the parse tree)

3. Run compiler using
cat example\_queries/flowlet\_hist.sql | java -ea Compiler

4. Run interpreter using
cat example\_queries/flowlet\_hist.sql | java -ea Interpreter 2> /tmp/output.py

5. Run interpreted code using
python3 /tmp/output.py
