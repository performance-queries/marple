def outofseq([lastseq, oos_count], [tcpseq, payloadlen]):
  if lastseq != tcpseq { oos_count = oos_count + 1; }
  tmp = tcpseq + payloadlen;
  lastseq = tmp;

tcp_pkts = filter(T, proto == 6);
oos_query = groupby(tcp_pkts,
                    [srcip, switch, dstip, srcport, dstport, proto],
                    outofseq);
