def e2esum([e2e_latency], [tout, tin]):
  e2e_latency = e2e_latency + (tout - tin)
  emit;

def empty([], []):
  ;

R1 = SELECT e2esum FROM T SGROUPBY [uid];
R3 = SELECT * FROM R2 WHERE e2e_latency > L;
R4 = SELECT empty FROM R3 RGROUPBY [srcip, dstip, srcport, dstport, proto];
