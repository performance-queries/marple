def new_flow ([perflowcount], []):
    if perflowcount == 0 { emit(); perflowcount = 1; }
    else { emit(); }

def flow_count ([portpaircount], [perflowcount]):
    portpaircount = portpaircount + (1 - perflowcount)
    emit()

def maxfc ([maxportcount], [portpaircount]):
    if maxportcount == 0 or maxportcount < portpaircount { maxportcount = portpaircount; }
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
