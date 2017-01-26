def total_counter([total_count], [uid]):
  total_count = total_count + 1
  emit()

def loss_counter([loss_count], [uid]):
  loss_count  = loss_count + 1
  emit()

total_counts  = groupby(T, [srcip, dstip, srcport, dstport, proto, switch], total_counter);
lost_pkts     = filter(T, tout == infinity);
lost_counts   = groupby(lost_pkts, [srcip, dstip, srcport, dstport, proto, switch], loss_counter);
joined_stream = zip(total_counts, lost_counts);
lc = filter(joined_stream, 100*loss_count > 1*total_count);
