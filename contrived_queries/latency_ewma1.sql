def ewma([avg, lastval, last2val, somefun, some2fun, avg2], [tin, tout]):
  alpha = 2/10;
  some2fun = lastval*2 + last2val*5 + somefun;
  somefun = tout*2 + last2val*5;
  last2val = lastval;
  lastval = tout-tin;
  avg2 = ((1 - alpha)/2) * (tout - tin) + ((1 - alpha)/2) * lastval;
  avg = alpha * avg + ((1 - alpha)/2) * (tout - tin) + ((1 - alpha)/2) * lastval;
  some2fun = avg + alpha;

ewma_query = groupby(T, [srcip, dstip, srcport, switch, dstport, proto], ewma);
