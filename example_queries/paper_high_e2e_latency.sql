def e2esum([e2e_latency], [tout, tin]):
  e2e_latency = e2e_latency + (tout - tin);

e2e = groupby(T, [srcip, dstip, srcport, dstport, proto, uid], e2esum);
