def perc([tot, high], [qin]):
  if qin > K then {high = high + 1}
  tot = tot + 1;

R1 = SELECT perc FROM T GROUPBY [qid];
R2 = SELECT [qid, tot, high] FROM R1 AS [tot, high];
R3 = SELECT * FROM R2 WHERE (high * 100) / tot > 1;
