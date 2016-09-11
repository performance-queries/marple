def perc([tot, high], [qin]):
  if (qin > 1000) { high = high + 1; }
  tot = tot + 1;
  emit();

R1 = SELECT perc FROM T GROUPBY [qid];
R2 = SELECT * FROM R1 WHERE (high * 100) / tot > 1;
