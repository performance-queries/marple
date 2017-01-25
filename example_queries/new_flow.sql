def new_flow([count], []):
  if count == 0 {
    emit();
    count = 1;
  }

result = groupby(T, [srcip, dstip, switch], new_flow);
