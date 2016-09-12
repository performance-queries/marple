def e2esum([e2e_latency, last_switch], [tout, tin, switch_id]):
  e2e_latency = e2e_latency + (tout - tin);
  last_switch = switch_id;
  emit();

def empty([], []):
  ;

R1 = groupby(T, [uid], e2esum);
R3 = filter(R1, e2e_latency > L and last_switch == egress_switch);
R4 = groupby(R3, [srcip, dstip, srcport, dstport, proto], empty);
