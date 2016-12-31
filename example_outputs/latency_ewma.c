// Packet fields
struct Packet {
  int _ewma_query_valid;
  int avg;
  int _pred_1;
  int _T_valid;
  int alpha;
  int tin;
  int tout;
}

// State declarations
int avg = 0;

// Fold function definition
void func(struct Packet pkt) {
  pkt._ewma_query_valid = 0;
  pkt._pred_1 = 1;
  pkt.alpha = (2)/(10);
  avg = (((pkt.alpha)*(avg))+((1)-(pkt.alpha)))*((pkt.egress_timestamp)-(pkt.ingress_timestamp));
}

