def nonmt([maxseq, nm_count], [tcpseq]):
  if maxseq > tcpseq : nm_count = nm_count + 1;
  if maxseq < tcpseq : maxseq = tcpseq;

tcp_pkts = SELECT * FROM T WHERE proto == TCP;
nmo_query = SELECT nonmt
            FROM tcp_pkts RGROUPBY [srcip, dstip, srcport, dstport, proto];
