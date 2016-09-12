def outofseq([lastseq, oos_count], [tcpseq]):
  if lastseq + 1 != tcpseq { oos_count = oos_count + 1; }
  lastseq = tcpseq + payload_len;

tcp_pkts = filter(T, proto == TCP);
oos_query = groupby(tcp_pkts,
                    [srcip, dstip, srcport, dstport, proto],
                    outofseq);
