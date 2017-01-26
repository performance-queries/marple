def timeout([count, last_time], [tin]):
  timediff = tin - last_time;
  if timediff > 200 and timediff < 320 {
    count = count + 1;
  }
  last_time = tin;

toq = groupby(T, [srcip, dstip, srcport, dstport, proto, switch], timeout);
