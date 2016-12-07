def assoc count ([counter], [pktlen]):
  counter = counter + pktlen

result = groupby(T, [srcip, dstip, srcport, dstport, proto], count);
