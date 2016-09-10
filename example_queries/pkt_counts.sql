def count ([counter], [uid]):
  counter = counter + 1

result = SELECT count FROM T RGROUPBY [srcip, dstip, srcport, dstport, proto];
