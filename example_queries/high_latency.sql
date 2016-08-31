highlatency = SELECT [srcip, dstip, srcport, dstport, proto, qid] FROM T WHERE tout - tin > 1;
