def fl_detect ([last_time, size], [tin]):
    delta = 5;
    if (tin - last_time > delta) { emit(); size = 1; }
    else { size = size + 1; }
    last_time = tin

def flsum ([sum], [size]):
    sum = sum + size

R1 = groupby(T, [switch, srcip, dstip, srcport, dstport, proto], fl_detect);
result = groupby(R1, [], flsum);
