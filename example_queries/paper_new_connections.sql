def new_flow ([count], []):
    if count == 0 { emit(); count = 1; }

def flow_count ([f_count], []):
    f_count = f_count + 1
    emit()

R1 = map(T, [epoch], [tin/128]);
R2 = groupby(R1, [srcip, dstip, srcport, dstport, proto, epoch, switch], new_flow);
R3 = groupby(R2, [epoch], flow_count);
