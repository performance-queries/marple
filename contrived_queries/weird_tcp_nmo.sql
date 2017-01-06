def nonmt([maxseq, nm_count], [tcpseq]):
  if maxseq > tcpseq { nm_count = nm_count + 1; }
  if maxseq < tcpseq { maxseq = tcpseq; }

tcp_pkts = filter(T, proto == 6);
nmo_query = groupby(tcp_pkts, [srcip, dstip, srcport, switch, dstport, proto], nonmt);
