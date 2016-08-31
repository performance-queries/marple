def ewma([avg], [tin, tout]):
  avg = alpha * avg + (1 - alpha) * (tout - tin)

ewma_query = SELECT [ewma] GROUPBY [srcip, dstip, srcport, dstport, proto] FROM T;
