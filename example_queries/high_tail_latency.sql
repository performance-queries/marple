def perc([tot, high], [qin]):
  if (qin > 1000) { high = high + 1; }
  tot = tot + 1;
  emit();

R1 = groupby(T, [qid, switch], perc);
R2 = filter(R1, (high * 100) > tot);
