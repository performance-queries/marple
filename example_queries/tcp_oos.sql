def outofseq([lastseq, oos_count], [tcpseq]):
  if lastseq + 1 != tcpseq then
    {oos_count = oos_count + 1;}
  lastseq = tcpseq + payload_len;

tcp_pkts = SELECT * FROM T WHERE proto == TCP;
oos_query = SELECT outofseq
            FROM tcp_pkts
            RGROUPBY [srcip, dstip, srcport, dstport, proto];
