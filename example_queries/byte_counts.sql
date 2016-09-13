def assoc count ([counter], [pkt_len]):
  counter = counter + pkt_len

result = groupby(T, [srcip, dstip, srcport, dstport, proto], count);
