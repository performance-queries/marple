def path_change([_s_last_path, _s_ch_count], [pkt_path]):
  if pkt_path != _s_last_path { _s_ch_count = _s_ch_count + 1; }
  _s_last_path = pkt_path

temp = SELECT path_change FROM T RGROUPBY [srcip, dstip, srcport, dstport, proto];
