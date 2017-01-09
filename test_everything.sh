#! /bin/bash
set -e;
if [ $# -ne 1 ]; then
  echo "Usage: ./test_everything.sh jar_file";
  exit
fi

# Hack: Domino binary and atom paths are defined in domino_paths.sh.
source domino_paths.sh;

for f in example_queries/*.sql; do
  echo "Testing $f ...";
  cat $f | java -ea -jar $1 ;
  # Testing with domino
  $DOMINO_BIN domino-full.c ${DOMINO_ATOMS}/nested_ifs.sk 16 10 ;
done

echo ;
echo "No smoking guns.";
