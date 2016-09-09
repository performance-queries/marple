def nonmt([_s_maxseq, _s_nm_count], [tcpseq]):
  if _s_maxseq > tcpseq { _s_nm_count = _s_nm_count + 1; }
  if _s_maxseq < tcpseq { _s_maxseq = tcpseq; }

tcp_pkts = SELECT * FROM T WHERE proto == TCP;
nmo_query = SELECT nonmt
            FROM tcp_pkts RGROUPBY [srcip, dstip, srcport, dstport, proto];
