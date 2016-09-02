def count ([counter], [pkt_len]):
  counter = counter + pkt_len

result = SELECT count FROM T GROUPBY [srcip, dstip, srcport, dstport, proto];
