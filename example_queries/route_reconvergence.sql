def path_change([last_path, ch_count], [pkt_path]):
  if pkt_path != last_path then ch_count = ch_count + 1
  last_path = pkt_path

result = SELECT [srcip, dstip, srcport, dstport, proto, path_change] FROM T GROUPBY [srcip, dstip, srcport, dstport, proto];
