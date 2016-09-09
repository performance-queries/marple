def total_counter([_s_total_count], [uid]):
  _s_total_count = _s_total_count + 1
  emit()

def loss_counter([_s_loss_count], [uid]):
  _s_loss_count  = _s_loss_count + 1
  emit()

total_counts  = SELECT total_counter FROM T SGROUPBY [srcip, dstip, srcport, dstport, proto];
lost_pkts     = SELECT * FROM T WHERE tout == INFINITY;
lost_counts   = SELECT loss_counter  FROM lost_pkts
                SGROUPBY [srcip, dstip, srcport, dstport, proto];
joined_stream = total_counts JOIN lost_counts;
result = SELECT [loss_count / total_count] FROM joined_stream AS [loss_rate];
