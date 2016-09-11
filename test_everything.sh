#! /bin/bash
set -e;
if [ $# -ne 1 ]; then
  echo "Usage: ./test_everything.sh module"; 
  exit
fi

for f in example_queries/*.sql; do
  cat $f | java -ea $1 ;
done 
