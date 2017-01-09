#! /bin/bash
set -e;
if [ $# -ne 1 ]; then
  echo "Usage: ./test_everything.sh jar_file";
  exit
fi

for f in example_queries/*.sql; do
  echo "Testing $f ...";
  cat $f | java -ea -jar $1 ;
done
