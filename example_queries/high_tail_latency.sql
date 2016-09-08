def perc([_s_tot, _s_high], [qin]):
  if (qin > K): _s_high = _s_high + 1;
  _s_tot = _s_tot + 1;
  emit();

R1 = SELECT perc FROM T SGROUPBY [qid];
R2 = SELECT * FROM R1 WHERE (s_high * 100) / s_tot > 1;
