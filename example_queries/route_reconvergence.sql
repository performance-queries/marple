def path_change([last_path, ch_count], [pkt_path]):
  if pkt_path != last_path : ch_count = ch_count + 1;
  last_path = pkt_path

temp = SELECT path_change FROM T RGROUPBY [srcip, dstip, srcport, dstport, proto];
