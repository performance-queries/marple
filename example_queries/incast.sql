def new_flow ([count], []):
    if count == 0 { emit(); count = 1; }

def flow_count ([f_count], []):
    f_count = f_count + 1
    emit()

R1 = map(T, [srcip, dstip, srcport, dstport, proto, epoch, switch],
         [srcip, dstip, srcport, dstport, proto, tin/128, switch]);
R2 = groupby(R1, [srcip, dstip, srcport, dstport, proto, epoch, switch], new_flow);
R3 = groupby(R2, [epoch], flow_count);
R4 = zip(R3, T);
result = filter(R4, qlen > 100 and f_count > 50);
