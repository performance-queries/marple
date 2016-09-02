def count ([counter], [uid]):
  counter = counter + 1

result = SELECT count FROM T GROUPBY [srcip, dstip, srcport, dstport, proto];
