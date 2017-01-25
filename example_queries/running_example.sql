def oos_count([count, lastseq], [tcpseq, payloadlen]):
  if lastseq != tcpseq {
    count = count + 1;
    emit();
  }
  lastseq = tcpseq + payloadlen

tcps    = filter(T, proto == 6
                 and (switch == 1 or switch == 2));
tslots  = map(T, [epoch], [tin/128]);
joined  = zip(tcps, tslots);
oos     = groupby(joined,
                  [srcip, dstip, srcport, dstport, switch, epoch],
                  oos_count);
