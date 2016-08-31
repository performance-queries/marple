def perc([tot, high], [qin]):
  if qin > K then high = high + 1
  tot = tot + 1;

R1 = SELECT [qid, perc] GROUPBY [qid] FROM T;
R2 = SELECT * FROM R1 WHERE (high * 100) / tot > 1;
