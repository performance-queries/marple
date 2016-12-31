// Packet fields
struct Packet {
  int _pred_3;
  int size;
  int _R1_valid;
  int last_time;
  int _pred_1;
  int _T_valid;
  int _pred_2;
  int tin;
  int delta;
  int _pred_4;
  int sum;
  int _result_valid;
}

// State declarations
int size = 0;
int last_time = 0;
int sum = 0;

// Fold function definition
void func(struct Packet pkt) {
  pkt._R1_valid = 0;
  pkt._pred_1 = 1;
  pkt.delta = 5;
  pkt._pred_2 = ((pkt.ingress_timestamp)-(last_time)) > (pkt.delta);
  pkt._R1_valid = (pkt._pred_2) || (pkt._R1_valid);
  pkt.size = pkt._pred_2 ? (size) : (pkt.size);
  pkt.last_time = pkt._pred_2 ? (last_time) : (pkt.last_time);
  size = pkt._pred_2 ? (1) : (size);
  pkt._pred_3 = ! (((pkt.ingress_timestamp)-(last_time)) > (pkt.delta));
  size = pkt._pred_3 ? ((size)+(1)) : (size);
  last_time = pkt.ingress_timestamp;
}

