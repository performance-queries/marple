#! /bin/bash
set -e;
if [ $# -ne 1 ]; then
  echo "Usage: ./test_domino.sh jar_file";
  exit
fi

# Hack: Domino binary and atom paths are defined in domino_paths.sh.
source domino_paths.sh;

for f in example_queries/*.sql; do
  echo "Testing $f ...";
  cat $f | java -ea -jar $1 ;
  # Testing with domino
  CURRDIR=`pwd`
  cd $DOMINO_EXAMPLES
  $DOMINO_BIN ${CURRDIR}/domino-full.c ${DOMINO_ATOMS}/mul_acc.sk 16 10 ;
  $DOMINO_BIN ${CURRDIR}/domino-full.c ${DOMINO_ATOMS}/nested_ifs.sk 16 10 ;
  cd -
done

echo ;
echo "No smoking guns.";
