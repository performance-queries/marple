def new_flow ([perflowcount], []):
    if perflowcount == 0 then { perflowcount = 1 };
    emit

def flow_count ([portpaircount], [perflowcount]):
    portpaircount = portpaircount + perflowcount
    emit

def maxfc ([maxportcount], [portpaircount]):
    if maxportcount == 0 || maxportcount < portpaircount then
       { maxportcount = portpaircount; }
    emit

R1 = select [srcip, dstip, srcport, dstport, proto, tin/128, inport, outport]
     from T as [srcip, dstip, srcport, dstport, proto, epoch, inp, outp];
R2 = select new_flow from R1 groupby [srcip, dstip, srcport, dstport, proto,
     	    	     	     	          epoch, inp, outp];
R3 = select flow_count from R2 groupby [epoch, inp, outp];
R4 = select maxfc from R3 groupby [outp, epoch];
R5 = R3 JOIN R4;
R6 = R5 JOIN T;
result = select * from R6 where maxportcount/portpaircount > 5 && qin > 100;
