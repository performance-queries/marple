def path_change([last_path, ch_count], [pktpath]):
  if pktpath != last_path { ch_count = ch_count + 1; }
  last_path = pktpath

temp = groupby(T, [srcip, dstip, srcport, dstport, proto, switch], path_change);
