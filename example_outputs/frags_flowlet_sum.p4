=================================
bit<32> switch;
bit<32> payload_length;
bit<32> qid;
bit<32> ingress_timestamp;
bit<32> egress_timestamp;
bit<32> queue_len;
=================================
bit<1> _R1_valid;
bit<32> size;
bit<1> _T_valid;
bit<32> last_time;
bit<32> sum;
bit<1> _result_valid;
=================================
[size, last_time, sum]
=================================
R1
[srcip, size, dstport, last_time, proto, srcport, dstip, switch]
GROUPBY
[switch, srcip, dstip, srcport, dstport, proto]
[last_time, size]
--
bit<1> _pred_1;
bit<32> delta;
bit<1> _pred_2;
bit<1> _pred_3;
meta._R1_valid = false;
_pred_1 = true;
delta = 32w5;
_pred_2 = ((pktlog_meta.ingress_timestamp)-(_val_last_time)) > (delta);
meta._R1_valid = (_pred_2) || (meta._R1_valid);
meta.size = _pred_2 ? (_val_size) : (meta.size);
meta.last_time = _pred_2 ? (_val_last_time) : (meta.last_time);
_val_size = _pred_2 ? (32w1) : (_val_size);
_pred_3 = ! (((pktlog_meta.ingress_timestamp)-(_val_last_time)) > (delta));
_val_size = _pred_3 ? ((_val_size)+(32w1)) : (_val_size);
_val_last_time = pktlog_meta.ingress_timestamp;
=================================
result
[sum]
GROUPBY
[]
[sum]
--
bit<1> _pred_4;
meta._result_valid = false;
_pred_4 = meta._R1_valid;
_val_sum = (_val_sum)+(meta.size);
=================================
