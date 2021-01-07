/* -*- P4_16 -*- */
#include <core.p4>
#include <v1model.p4>

#define MAX_PORTS 255

const bit<16> ETHERTYPE_ARP  = 0x0806;
const bit<16> TYPE_IPV4 = 0x800;

/* IP protocols */
const bit<8> IP_PROTOCOLS_ICMP       =   1;
const bit<8> IP_PROTOCOLS_IGMP       =   2;
const bit<8> IP_PROTOCOLS_IPV4       =   4;
const bit<8> IP_PROTOCOLS_TCP        =   6;
const bit<8> IP_PROTOCOLS_UDP        =  17;
const bit<8> IP_PROTOCOLS_IPV6       =  41;
const bit<8> IP_PROTOCOLS_GRE        =  47;
const bit<8> IP_PROTOCOLS_IPSEC_ESP  =  50;
const bit<8> IP_PROTOCOLS_IPSEC_AH   =  51;
const bit<8> IP_PROTOCOLS_ICMPV6     =  58;
const bit<8> IP_PROTOCOLS_EIGRP      =  88;
const bit<8> IP_PROTOCOLS_OSPF       =  89;
const bit<8> IP_PROTOCOLS_PIM        = 103;
const bit<8> IP_PROTOCOLS_VRRP       = 112;


/*************************************************************************
*********************** H E A D E R S  ***********************************
*************************************************************************/

typedef bit<16>  port_t;
typedef bit<48> mac_addr_t;
typedef bit<32> ip4_addr_t;



header ethernet_t {
    mac_addr_t dst_addr;
    mac_addr_t src_addr;
    bit<16>   ether_type;
}

header ipv4_t {
    bit<4>    version;
    bit<4>    ihl;
    bit<6>    diffserv;
    bit<2>    ecn;
    bit<16>   totalLen;
    bit<16>   identification;
    bit<3>    flags;
    bit<13>   frag_offset;
    bit<8>    ttl;
    bit<8>    protocol;
    bit<16>   hdr_checksum;
    ip4_addr_t src_addr;
    ip4_addr_t dst_addr;
}

header tcp_t {
    bit<16> src_port;
    bit<16> dst_port;
    bit<32> seq_no;
    bit<32> ack_no;
    bit<4>  data_offset;
    bit<3>  res;
    bit<3>  ecn;
    bit<6>  ctrl;
    bit<16> window;
    bit<16> checksum;
    bit<16> urgent_ptr;
}

header udp_t {
    bit<16> src_port;
    bit<16> dst_port;
    bit<16> len;
    bit<16> checksum;
}

struct metadata {
    bit<16> tcp_length;
}


struct headers {
    ethernet_t   ethernet;
    ipv4_t       ipv4;
    tcp_t tcp;
    udp_t udp;
}

/*************************************************************************
*********************** P A R S E R  ***********************************
*************************************************************************/

parser ParserImpl(packet_in packet,
                out headers hdr,
                inout metadata meta,
                inout standard_metadata_t standard_metadata) {

    state start {
        transition parse_ethernet;
    }

    state parse_ethernet {
        packet.extract(hdr.ethernet);
        transition select(hdr.ethernet.ether_type) {
            TYPE_IPV4: parse_ipv4;
            default: accept;
        }
    }

    state parse_ipv4 {
        packet.extract(hdr.ipv4);
        transition select(hdr.ipv4.protocol) {
            IP_PROTOCOLS_TCP: parse_tcp;
            IP_PROTOCOLS_UDP: parse_udp;
            default: accept;
        }
    }

    state parse_tcp {
        packet.extract(hdr.tcp);
        meta.tcp_length = hdr.ipv4.totalLen - 16w20;
        transition accept;
    }

    state parse_udp {
        packet.extract(hdr.udp);
        transition accept;
    }
}


/*************************************************************************
************   C H E C K S U M    V E R I F I C A T I O N   *************
*************************************************************************/

control VerifyChecksumImpl(inout headers hdr, inout metadata meta) {
    apply {  }
}


/*************************************************************************
**************  I N G R E S S   P R O C E S S I N G   *******************
*************************************************************************/

control IngressPipeImpl(inout headers hdr,
                  inout metadata meta,
                  inout standard_metadata_t standard_metadata) {


    direct_counter(CounterType.packets_and_bytes) t_acl_counter;

    action allow() {
        t_acl_counter.count();
    }

    action deny() {
        mark_to_drop();
        t_acl_counter.count();
    }

    action nop() {
        t_acl_counter.count();
    }

    table t_acl {
        key = {
            standard_metadata.ingress_port   :   ternary;
	        hdr.ethernet.src_addr	         :   ternary;
            hdr.ethernet.dst_addr	         :   ternary;
            hdr.ethernet.ether_type    	     :   ternary;
            hdr.ipv4.src_addr                :   ternary;
            hdr.ipv4.dst_addr                :   ternary;
      	    hdr.ipv4.protocol		         :   ternary;
            hdr.tcp.src_port                 :   ternary;
            hdr.tcp.dst_port                 :   ternary;
      	    hdr.udp.src_port                 :   ternary;
      	    hdr.udp.dst_port                 :   ternary;
        }
        actions = {
            allow;
            deny;
            nop;
        }
        size = 5000;
        default_action = nop();
        counters = t_acl_counter;
    }

    /*
    direct_counter(CounterType.packets_and_bytes) t_fwd_counter;

    action drop() {
        mark_to_drop();
        t_fwd_counter.count();
    }

    action fwd(port_t port) {
        standard_metadata.egress_spec = port;
        t_fwd_counter.count();
    }

    table t_fwd {
        key = {
            standard_metadata.ingress_port   :   exact;
        }
        actions = {
            fwd;
            drop;
        }
        default_action = drop();
        counters = t_fwd_counter;

        const entries = {
            (0x0) : fwd(1);
            (0x1) : fwd(0);
        }
    }
    */

    apply {

        switch(t_acl.apply().action_run) {
            deny: {
                return;
            }
            default: {

            }
        }

        //t_fwd.apply();
        if (standard_metadata.ingress_port == 0) {
            standard_metadata.egress_spec = 1;
        } else {
            standard_metadata.egress_spec = 0;
        }

    }
}

/*************************************************************************
****************  E G R E S S   P R O C E S S I N G   *******************
*************************************************************************/

control EgressPipeImpl(inout headers hdr,
                 inout metadata meta,
                 inout standard_metadata_t standard_metadata) {
    apply {
    }
}

/*************************************************************************
*************   C H E C K S U M    C O M P U T A T I O N   **************
*************************************************************************/

control ComputeChecksumImpl(inout headers hdr, inout metadata meta) {
     apply {
	update_checksum(
	    hdr.ipv4.isValid(),
            { hdr.ipv4.version,
	          hdr.ipv4.ihl,
	          hdr.ipv4.diffserv,
	          hdr.ipv4.ecn,
              hdr.ipv4.totalLen,
              hdr.ipv4.identification,
              hdr.ipv4.flags,
              hdr.ipv4.frag_offset,
              hdr.ipv4.ttl,
              hdr.ipv4.protocol,
              hdr.ipv4.src_addr,
              hdr.ipv4.dst_addr },
              hdr.ipv4.hdr_checksum,
              HashAlgorithm.csum16);
        update_checksum_with_payload(hdr.udp.isValid(),
            { hdr.ipv4.src_addr,
              hdr.ipv4.dst_addr,
              8w0,
              hdr.ipv4.protocol,
              hdr.udp.len,
              hdr.udp.src_port,
              hdr.udp.dst_port,
              hdr.udp.len },
              hdr.udp.checksum, HashAlgorithm.csum16);
        update_checksum_with_payload(hdr.tcp.isValid(),
            { hdr.ipv4.src_addr,
              hdr.ipv4.dst_addr,
              8w0,
              hdr.ipv4.protocol,
              meta.tcp_length,
              hdr.tcp.src_port,
              hdr.tcp.dst_port,
              hdr.tcp.seq_no,
              hdr.tcp.ack_no,
              hdr.tcp.data_offset,
              hdr.tcp.res,
              hdr.tcp.ecn,
              hdr.tcp.ctrl,
              hdr.tcp.window,
              hdr.tcp.urgent_ptr },
              hdr.tcp.checksum, HashAlgorithm.csum16);
    }
}

/*************************************************************************
***********************  D E P A R S E R  *******************************
*************************************************************************/

control DeparserImpl(packet_out packet, in headers hdr) {
    apply {
        packet.emit(hdr.ethernet);
        packet.emit(hdr.ipv4);
        packet.emit(hdr.tcp);
        packet.emit(hdr.udp);
    }
}

/*************************************************************************
***********************  S W I T C H  *******************************
*************************************************************************/

V1Switch(
ParserImpl(),
VerifyChecksumImpl(),
IngressPipeImpl(),
EgressPipeImpl(),
ComputeChecksumImpl(),
DeparserImpl()
) main;
