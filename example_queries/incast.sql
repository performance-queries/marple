def new_flow ([count], []):
    if count == 0 { emit(); count = 1; }

def flow_count ([f_count], []):
    f_count = f_count + 1
    emit()

R1 = select [srcip, dstip, srcport, dstport, proto, tin/128] from T as
     [srcip, dstip, srcport, dstport, proto, epoch];
R2 = select new_flow from R1 GROUPBY [srcip, dstip, srcport, dstport, proto, epoch];
R3 = select flow_count from R2 GROUPBY [epoch];
R4 = R3 JOIN T;
result = select * from R4 where qin > 100 and f_count > 50;
