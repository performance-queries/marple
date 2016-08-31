def count ([counter], [uid]):
  counter = counter + 1

temp = SELECT [count] GROUPBY [srcip, dstip, srcport, dstport, proto] FROM T; 
