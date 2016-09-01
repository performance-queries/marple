R1 = SELECT * FROM T WHERE tout - tin > 1;
R2 = SELECT [srcip, dstip, srcport, dstport, proto, qid] FROM R1 AS [srcip, dstip, srcport, dstport, proto, qid];
