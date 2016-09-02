def total_counter([total_count], [uid]):
  total_count = total_count + 1
  emit

def loss_counter([loss_count], [uid]):
  loss_count  = loss_count + 1
  emit

total_counts  = SELECT total_counter FROM T GROUPBY [srcip, dstip, srcport, dstport, proto];
lost_pkts     = SELECT * FROM T WHERE tout == INFINITY;
lost_counts   = SELECT loss_counter  FROM lost_pkts
                GROUPBY [srcip, dstip, srcport, dstport, proto];
joined_stream = total_counts JOIN lost_counts;
result = SELECT [loss_count / total_count] FROM joined_stream AS [loss_rate];
