def count ([_s_counter], [pkt_len]):
  _s_counter = _s_counter + pkt_len

result = SELECT count FROM T RGROUPBY [srcip, dstip, srcport, dstport, proto];
