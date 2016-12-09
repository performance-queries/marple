def e2esum([e2e_latency, last_switch], [tout, tin, switch]):
  e2e_latency = e2e_latency + (tout - tin);
  last_switch = switch;
  emit();

def assoc empty([], []):
  ;

R1 = groupby(T, [srcip, dstip, srcport, dstport, proto, uid], e2esum);
R3 = filter(R1, e2e_latency > 1000 and last_switch == 20);
R4 = groupby(R3, [srcip, dstip, srcport, dstport, proto], empty);
