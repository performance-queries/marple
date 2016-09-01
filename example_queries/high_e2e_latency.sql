def e2esum([e2e_latency], [tout, tin]):
  e2e_latency = e2e_latency + (tout - tin)

R1 = SELECT [uid, e2esum] FROM T GROUPBY [uid];
R2 = SELECT * FROM R1 WHERE e2esum > L;
R3 = SELECT [srcip, dstip, srcport, dstport, proto] FROM R2 GROUPBY [srcip, dstip, srcport, dstport, proto];
