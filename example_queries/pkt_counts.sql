def assoc count ([counter], [uid]):
  counter = counter + 1

result = groupby(T, [srcip, dstip, srcport, dstport, proto], count);
