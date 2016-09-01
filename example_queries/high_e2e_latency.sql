def e2esum([e2e_latency], [tout, tin]):
  e2e_latency = e2e_latency + (tout - tin)

def empty([dummy], [tout]):
  ;

R1 = SELECT e2esum FROM T GROUPBY [uid];
R2 = SELECT [uid, e2e_latency] FROM R1 AS [uid, e2e_latency];
R3 = SELECT * FROM R2 WHERE e2e_latency > L;
R4 = SELECT empty FROM R3 GROUPBY [srcip, dstip, srcport, dstport, proto];
R5 = SELECT [srcip, dstip, srcport, dstport, proto] FROM R4
     AS [srcip, dstip, srcport, dstport, proto];
