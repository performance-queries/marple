def count ([counter], [pkt_len]):
  counter = counter + pkt_len

temp = SELECT * FROM T GROUPBY [srcip, dstip, srcport, dstport, proto];
result = SELECT [counter] FROM temp AS [counter];
