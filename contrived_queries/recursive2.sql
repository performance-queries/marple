def rec_fun([maxseq,
             state1, state2, state3, state4,
             state5, state6, state7, state8,
             state9, state10],
             [tcpseq, dstip]):
  state6 = state8;
  state8 = state7;
  state7 = tcpseq;
  state9 = state7;
  if (tcpseq > dstip) {
    state1 = 4;
    if (tcpseq > dstip + 10) {
      state2 = state1 + 5;
      state1 = state2;
      state10 = state6;
    } else {
      state3 = 5;
      state9 = state4 + 4;
      if (tcpseq > maxseq + 5) {
        state4 = state3 + 2;
        state1 = tcpseq;
      }
      state10 = state6;
      state2 = tcpseq + 3;
    }
  } else {
    state5 = state4;
    state10 = state6;
    state2 = 5;
    state1 = dstip;
  }
  state5 = state1 + state2 + state3 + state4;

recursive_query = groupby(T, [srcip, switch], rec_fun);
