def count([cnt], []):
    cnt = cnt + 1;
    emit();


R1 = groupby(T, [switch, tin], count);
