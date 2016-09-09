def perc([_s_tot, _s_high], [qin]):
  if (qin > 1000) { _s_high = _s_high + 1; }
  _s_tot = _s_tot + 1;
  emit();

R1 = SELECT perc FROM T SGROUPBY [qid];
R2 = SELECT * FROM R1 WHERE (high * 100) / tot > 1;
