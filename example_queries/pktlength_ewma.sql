def ewma([avg], [payloadlen]):
  alpha = 3/16;
  avg = (alpha * avg) + ((1 - alpha) * payloadlen)

ewma_query = groupby(T, [srcip, dstip, srcport, switch, dstport, proto], ewma);
