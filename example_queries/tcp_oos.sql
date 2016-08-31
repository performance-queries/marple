def outofseq([lastseq, oos_count], [tcpseq]):
  if lastseq + 1 != tcpseq then
    oos_count = oos_count + 1;
  lastseq = tcpseq + payload_len;

oos_query = SELECT [srcip, dstip, srcport, dstport, proto, outofseq] GROUPBY [srcip, dstip, srcport, dstport, proto] FROM T WHERE proto == TCP;
