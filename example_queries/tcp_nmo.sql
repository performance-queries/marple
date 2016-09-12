def nonmt([maxseq, nm_count], [tcpseq]):
  if maxseq > tcpseq { nm_count = nm_count + 1; }
  if maxseq < tcpseq { maxseq = tcpseq; }

tcp_pkts = filter(T, proto == TCP);
nmo_query = groupby(tcp_pkts, [srcip, dstip, srcport, dstport, proto], nonmt);
