def loss_rate ([counter, loss_counter, loss_rate], [tout]):
  counter = counter + 1
  if tout == 5555 then loss_counter = loss_counter + 1
  loss_rate = loss_counter / counter

R1 = SELECT [loss_rate] GROUPBY [srcip, dstip, srcport, dstport, proto] FROM T;
