def fl_detect ([last_time, size], [tin]):
    delta = 5;
    if (tin - last_time > delta) { emit(); size = 1; }
    else { size = size + 1; }
    last_time = tin

def flsum ([sum], []):
    sum = sum + 1

R1 = groupby(T, [srcip, dstip, srcport, dstport, proto, switch], fl_detect);
R2 = map(R1, [size_index], [size/128]);
result = groupby(R2, [size_index], flsum);
