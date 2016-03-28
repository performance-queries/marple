#! /bin/bash

export CLASSPATH=".:./third_party/antlr-4.5.2-complete.jar:$CLASSPATH"
alias grun='java org.antlr.v4.gui.TestRig'
alias antlr4='java -jar ./third_party/antlr-4.5.2-complete.jar'
