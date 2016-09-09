def outofseq([_s_lastseq, _s_oos_count], [tcpseq]):
  if _s_lastseq + 1 != tcpseq { _s_oos_count = _s_oos_count + 1; }
  _s_lastseq = tcpseq + payload_len;

tcp_pkts = SELECT * FROM T WHERE proto == TCP;
oos_query = SELECT outofseq
            FROM tcp_pkts
            RGROUPBY [srcip, dstip, srcport, dstport, proto];
