def ewma([avg], [tin, tout]):
  alpha = 2/10;
  avg = alpha * avg + (1 - alpha) * (tout - tin)

ewma_query = groupby(T, [srcip, dstip, srcport, switch, dstport, proto], ewma);
