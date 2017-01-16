def new_flow ([perflowcount], []):
    if perflowcount == 0 { emit(); perflowcount = 1; }
    else { emit(); }

def flow_count ([portpaircount], [perflowcount]):
    portpaircount = portpaircount + (1 - perflowcount)
    emit()

def maxfc ([maxportcount], [portpaircount]):
    if maxportcount == 0 or maxportcount < portpaircount { maxportcount = portpaircount; }
    emit()

R1 = map(T, [epoch, inp, outp], [tin/128, inport, outport]);
R2 = groupby(R1, [srcip, dstip, srcport, dstport, proto, epoch, inp, outp, switch], new_flow);
R3 = groupby(R2, [epoch, inp, outp], flow_count);
R4 = groupby(R3, [outp, epoch], maxfc);
R5 = zip(R3, R4);
R6 = zip(R5, T);
result = filter(R6, maxportcount > 5*portpaircount and qin > 100);
