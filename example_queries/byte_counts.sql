def count ([counter], [pkt_len]):
  counter = counter + pkt_len

temp = SELECT count GROUPBY [srcip, dstip, srcport, dstport, proto] FROM T; 
