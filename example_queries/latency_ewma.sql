def ewma([_s_avg], [tin, tout]):
  _s_avg = alpha * _s_avg + (1 - alpha) * (tout - tin)

ewma_query = SELECT ewma FROM T RGROUPBY [srcip, dstip, srcport, dstport, proto];
