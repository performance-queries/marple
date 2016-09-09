def count ([_s_counter], [uid]):
  _s_counter = _s_counter + 1

result = SELECT count FROM T RGROUPBY [srcip, dstip, srcport, dstport, proto];
