def ewma([avg], [tin, tout]):
  avg = alpha * avg + (1 - alpha) * (tout - tin)

ewma_query = SELECT * FROM T GROUPBY [srcip, dstip, srcport, dstport, proto];
result     = SELECT [avg] FROM  ewma_query AS [ewma_query];
