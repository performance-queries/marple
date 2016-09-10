def fl_detect ([last_time, size], [tin]):
    delta = 5;
    if (tin - last_time > delta) { emit(); size = 1; }
    else { size = size + 1; }
    last_time = tin

def flsum ([sum], [size]):
    sum = sum + size

R1 = SELECT fl_detect FROM T SGROUPBY [srcip, dstip, srcport, dstport,
         proto];
R2 = SELECT [size/100] FROM R1 as [size_index];
result = SELECT flsum from R2 RGROUPBY [ size_index ];
