def perc([tot, high], [qin]):
  if (qin > K): high = high + 1;
  tot = tot + 1;
  emit();

R1 = SELECT perc FROM T SGROUPBY [qid];
R2 = SELECT * FROM R1 WHERE (high * 100) / tot > 1;
