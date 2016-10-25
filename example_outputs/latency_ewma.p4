#include <core.p4>
#include <v1model.p4>

struct ingress_metadata_t {
    bit<32> nhop_ipv4;
    /* perfq: additional headers for ewma */
    bit<16> avg;
    bit<16> ewma_offset;
}

struct intrinsic_metadata_t {
    bit<48> ingress_global_timestamp;
    bit<32> lf_field_list;
    bit<16> mcast_grp;
    bit<16> egress_rid;
    /* perfq: adding queue size here. */
    /* TODO: Not sure how the existence of intrinsic metadata is checked. */
    bit<16> queue_size;
}

header ethernet_t {
    bit<48> dstAddr;
    bit<48> srcAddr;
    bit<16> etherType;
}

header ipv4_t {
    bit<4>  version;
    bit<4>  ihl;
    bit<8>  diffserv;
    bit<16> totalLen;
    bit<16> identification;
    bit<3>  flags;
    bit<13> fragOffset;
    bit<8>  ttl;
    bit<8>  protocol;
    bit<16> hdrChecksum;
    bit<32> srcAddr;
    bit<32> dstAddr;
}

header tcp_t {
    bit<16> srcPort;
    bit<16> dstPort;
    bit<32> seqNo;
    bit<32> ackNo;
    bit<4>  dataOffset;
    bit<3>  res;
    bit<3>  ecn;
    bit<6>  ctrl;
    bit<16> window;
    bit<16> checksum;
    bit<16> urgentPtr;
}

struct metadata {
    @name("ingress_metadata")
    ingress_metadata_t   ingress_metadata;
    @name("intrinsic_metadata")
    intrinsic_metadata_t intrinsic_metadata;
}

struct headers {
    @name("ethernet")
    ethernet_t ethernet;
    @name("ipv4")
    ipv4_t     ipv4;
    @name("tcp")
    tcp_t      tcp;
}

parser ParserImpl (packet_in packet, out headers hdr, inout metadata meta, inout standard_metadata_t standard_metadata) {
    @name("parse_ethernet") state parse_ethernet {
        packet.extract(hdr.ethernet);
        transition select(hdr.ethernet.etherType) {
            16w0x800: parse_ipv4;
            default: accept;
        }
    }
    @name("parse_ipv4") state parse_ipv4 {
        packet.extract(hdr.ipv4);
        transition select(hdr.ipv4.protocol) {
            8w6: parse_tcp;
            default: accept;
        }
    }
    @name("parse_tcp") state parse_tcp {
        packet.extract(hdr.tcp);
        transition accept;
    }
    @name("start") state start {
        transition parse_ethernet;
    }
}

control egress(inout headers hdr, inout metadata meta, inout standard_metadata_t standard_metadata) {
    @name("rewrite_mac") action rewrite_mac(bit<48> smac) {
        hdr.ethernet.srcAddr = smac;
    }
    @name("_drop") action _drop() {
        mark_to_drop();
    }
    @name("send_frame") table send_frame() {
        actions = {
            rewrite_mac;
            _drop;
            NoAction;
        }
        key = {
            standard_metadata.egress_port: exact;
        }
        size = 256;
        default_action = NoAction();
    }
    apply {
        send_frame.apply();
    }
}

control ingress(inout headers hdr, inout metadata meta, inout standard_metadata_t standard_metadata) {
    /* perfq: register to hold EWMA values */
    @name("ewma_register") register<bit<16>>(32w8192) ewma_register;
    @name("_drop") action _drop() {
        mark_to_drop();
    }
    @name("set_nhop") action set_nhop() {
        bit<8> EGRESS_PORT = 8w2;
        standard_metadata.egress_spec = EGRESS_PORT;
        hdr.ipv4.ttl = hdr.ipv4.ttl + 8w255;
    }
    /* perfq: add new action for EWMA update */
    @name("ewma_action") action ewma_action() {
        hash(meta.ingress_metadata.ewma_offset, HashAlgorithm.crc16, (bit<16>)0,
          { hdr.ipv4.srcAddr, hdr.ipv4.dstAddr, hdr.tcp.srcPort,
            hdr.tcp.dstPort, hdr.ipv4.protocol }, (bit<20>)16384);
        ewma_register.read(meta.ingress_metadata.avg,
          (bit<32>)meta.ingress_metadata.ewma_offset);
        bit<16> alpha = 240;
        meta.ingress_metadata.avg = ((alpha * meta.intrinsic_metadata.queue_size) +
          ((255-alpha)* meta.ingress_metadata.avg))/256;
        ewma_register.write((bit<32>)meta.ingress_metadata.ewma_offset,
          meta.ingress_metadata.avg);
    }
    /* perfq: new table to read ewma per connection */
    @name("ewma_table") table ewma_table() {
        actions = {
            ewma_action;
            NoAction;
        }
        size = 16384;
        default_action = NoAction();
    }
    apply {
        @atomic {
            ewma_table.apply();
        }
        set_nhop();
    }
}

control DeparserImpl(packet_out packet, in headers hdr) {
    apply {
        packet.emit(hdr.ethernet);
        packet.emit(hdr.ipv4);
        packet.emit(hdr.tcp);
    }
}

control verifyChecksum(in headers hdr, inout metadata meta) {
    Checksum16() ipv4_checksum;
    apply {
        if (hdr.ipv4.hdrChecksum == ipv4_checksum.get({ hdr.ipv4.version, hdr.ipv4.ihl, hdr.ipv4.diffserv, hdr.ipv4.totalLen, hdr.ipv4.identification, hdr.ipv4.flags, hdr.ipv4.fragOffset, hdr.ipv4.ttl, hdr.ipv4.protocol, hdr.ipv4.srcAddr, hdr.ipv4.dstAddr }))
            mark_to_drop();
    }
}

control computeChecksum(inout headers hdr, inout metadata meta) {
    Checksum16() ipv4_checksum;
    apply {
        hdr.ipv4.hdrChecksum = ipv4_checksum.get({ hdr.ipv4.version, hdr.ipv4.ihl, hdr.ipv4.diffserv, hdr.ipv4.totalLen, hdr.ipv4.identification, hdr.ipv4.flags, hdr.ipv4.fragOffset, hdr.ipv4.ttl, hdr.ipv4.protocol, hdr.ipv4.srcAddr, hdr.ipv4.dstAddr });
    }
}

V1Switch(ParserImpl(), verifyChecksum(), ingress(), egress(), computeChecksum(), DeparserImpl()) main;
