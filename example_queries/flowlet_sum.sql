def fl_detect ([_s_last_time, _s_size], [tin]):
    delta = 5;
    if (tin - last_time > delta) { emit(); _s_size = 1; }
    else { _s_size = _s_size + 1; }
    _s_last_time = tin

def flsum ([_s_sum], [size]):
    _s_sum = _s_sum + size

R1 = SELECT fl_detect FROM T SGROUPBY [srcip, dstip, srcport, dstport,
         proto];
result = SELECT flsum from R1 RGROUPBY [];
