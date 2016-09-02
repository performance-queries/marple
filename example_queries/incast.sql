def new_flow ([count], []):
    if count == 0 then { emit; count = 1 };

def flow_count ([count], []):
    count = count + 1
    emit

R1 = select [srcip, dstip, srcport, dstport, proto, tin/128] from T as
     [srcip, dstip, srcport, dstport, proto, epoch];
R2 = select new_flow from R1 sgroupby [srcip, dstip, srcport, dstport, proto, epoch];
R3 = select flow_count from R2 sgroupby [epoch];
R4 = R3 JOIN T;
result = select * from R4 where qin > 100 && flow_count > 50;
