def agg([], []):
  ;

R1 = filter(T, tout - tin > 1);
R2 = groupby(R1, [srcip, dstip, srcport, dstport, proto, qid, switch], agg);
