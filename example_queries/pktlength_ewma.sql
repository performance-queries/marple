def ewma([avg], [payloadlen]):
  alpha = 1/4;
  avg = (alpha * avg) + ((1 - alpha) * payloadlen)

ewma_query = groupby(T, [srcip, dstip, srcport, switch, dstport, proto], ewma);
