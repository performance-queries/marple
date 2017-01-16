def ewma([avg, lastval, last2val, somefun, some2fun, avg2, avg3], [tin, tout]):
  alpha = 2/10;
  some2fun = lastval*2 + last2val*5 + somefun;
  somefun = tout*2 + last2val*5;
  last2val = lastval;
  lastval = tout-tin;
  avg3 = avg3 + 8;
  if last2val > lastval + tout {
    avg = (somefun * avg) + last2val;
    if avg3 == 4 {
      avg2 = avg2 + 4;
    }
  } else {
    avg = 2;
  }

ewma_query = groupby(T, [srcip, dstip, srcport, switch, dstport, proto], ewma);
