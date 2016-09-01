def nonmt([maxseq, nm_count], [tcpseq]):
  if maxseq > tcpseq then
    {nm_count = nm_count + 1}
  if maxseq < tcpseq then
    {maxseq = tcpseq}

tcp_pkts = SELECT * FROM T WHERE proto == TCP;
nmo_query = SELECT nonmt
            FROM tcp_pkts GROUPBY [srcip, dstip, srcport, dstport, proto];
result = SELECT [srcip, dstip, srcport, dstport, proto, nonmt]
         FROM nmo_query AS [srcip, dstip, srcport, dstport, proto, nonmt];
