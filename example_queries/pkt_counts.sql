def count ([counter], [uid]):
  counter = counter + 1

temp = SELECT [count] FROM T GROUPBY [srcip, dstip, srcport, dstport, proto];
