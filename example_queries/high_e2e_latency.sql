def e2esum([e2e_latency], [tout, tin]):
  e2e_latency = e2e_latency + (tout - tin)

R1 = SELECT [uid, e2esum] GROUPBY [uid] FROM T;
R2 = SELECT * FROM R1 WHERE e2esum > L;
R3 = SELECT [srcip, dstip, srcport, dstport, proto] GROUPBY [srcip, dstip, srcport, dstport, proto] FROM R2;
