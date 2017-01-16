def ewma([avg], [tin, tout]):
  alpha = 3/16;
  avg = (alpha * avg) + ((1 - alpha) * (tout - tin))

ewma_query = groupby(T, [srcip, dstip, srcport, switch, dstport, proto], ewma);
