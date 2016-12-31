=================================
bit<32> switch;
bit<32> payload_length;
bit<32> qid;
bit<32> ingress_timestamp;
bit<32> egress_timestamp;
bit<32> queue_len;
=================================
bit<1> _ewma_query_valid;
bit<32> avg;
bit<1> _T_valid;
=================================
[avg]
=================================
ewma_query
[srcip, avg, dstport, proto, srcport, dstip, switch]
GROUPBY
[srcip, dstip, srcport, switch, dstport, proto]
[avg]
--
bit<1> _pred_1;
bit<32> alpha;
meta._ewma_query_valid = false;
_pred_1 = true;
alpha = (32w2)/(32w10);
_val_avg = (((alpha)*(_val_avg))+((32w1)-(alpha)))*((pktlog_meta.egress_timestamp)-(pktlog_meta.ingress_timestamp));
=================================
