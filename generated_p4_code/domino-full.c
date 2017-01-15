// Packet fields
struct Packet {
  int qlen;
  int _tmp_R_valid;
  int _R_valid;
};

// State declarations

// Fold function definition
void func(struct Packet pkt) {
  pkt._tmp_R_valid = (pkt.qlen) < (5);
}
