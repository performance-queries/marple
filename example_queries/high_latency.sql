R1 = filter(T, tout - tin > 1);
R2 = map(R1, [srcip, dstip, srcport, dstport, proto, qid],
             [srcip, dstip, srcport, dstport, proto, qid]);
