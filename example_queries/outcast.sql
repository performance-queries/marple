def new_flow ([_s_perflowcount], []):
    if _s_perflowcount == 0 { emit(); _s_perflowcount = 1; }
    else: emit();

def flow_count ([_s_portpaircount], [perflowcount]):
    _s_portpaircount = _s_portpaircount + (1 - perflowcount)
    emit()

def maxfc ([_s_maxportcount], [portpaircount]):
    if _s_maxportcount == 0 or _s_maxportcount < portpaircount { _s_maxportcount = portpaircount; }
    emit()

R1 = select [srcip, dstip, srcport, dstport, proto, tin/128, inport, outport]
     from T as [srcip, dstip, srcport, dstport, proto, epoch, inp, outp];
R2 = select new_flow from R1 sgroupby [srcip, dstip, srcport, dstport, proto,
     	    	     	     	          epoch, inp, outp];
R3 = select flow_count from R2 sgroupby [epoch, inp, outp];
R4 = select maxfc from R3 sgroupby [outp, epoch];
R5 = R3 JOIN R4;
R6 = R5 JOIN T;
result = select * from R6 where maxportcount/portpaircount > 5 and qin > 100;
