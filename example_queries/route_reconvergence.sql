def path_change([last_path, ch_count], [pkt_path]):
  if pkt_path != last_path then {ch_count = ch_count + 1}
  last_path = pkt_path

temp = SELECT path_change FROM T GROUPBY [srcip, dstip, srcport, dstport, proto];
result = SELECT [srcip, dstip, srcport, dstport, proto, ch_count] FROM temp AS
         [srcip, dstip, srcport, dstport, proto, ch_count];
