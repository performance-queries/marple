def count ([counter], [pkt_len]):
  counter = counter + pkt_len

temp = SELECT count FROM T GROUPBY [srcip, dstip, srcport, dstport, proto];
result = SELECT [counter] FROM temp AS [counter];
