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
  pkt._result_valid = 0;
  pkt._pred_4 = pkt._R1_valid;
  sum = (sum)+(pkt.size);
}

