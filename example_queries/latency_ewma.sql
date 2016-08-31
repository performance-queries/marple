def ewma([avg], [tin, tout]):
  avg = alpha * avg + (1 - alpha) * (tout - tin)

ewmaquery = SELECT ewma GROUPBY [srcip, dstip, srcport, dstport, proto] FROM T;
