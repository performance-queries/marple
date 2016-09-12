def ewma([avg], [tin, tout]):
  avg = alpha * avg + (1 - alpha) * (tout - tin)

ewma_query = groupby(T, [srcip, dstip, srcport, dstport, proto], ewma);
