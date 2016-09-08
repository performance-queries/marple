def e2esum([_s_e2e_latency, _s_last_switch], [tout, tin, switch_id]):
  _s_e2e_latency = _s_e2e_latency + (tout - tin);
  _s_last_switch = switch_id;
  emit();

def empty([], []):
  ;

R1 = SELECT e2esum FROM T SGROUPBY [uid];
R3 = SELECT * FROM R1 WHERE e2e_latency > L and last_switch == egress_switch;
R4 = SELECT empty FROM R3 RGROUPBY [srcip, dstip, srcport, dstport, proto];
