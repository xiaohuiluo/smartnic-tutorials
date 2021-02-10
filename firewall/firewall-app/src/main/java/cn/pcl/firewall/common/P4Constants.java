package cn.pcl.firewall.common;

public interface P4Constants {

    /**
     * table
     */
    String TBL_ACL = "ingress::t_acl";
    String TBL_FWD = "ingress::t_fwd";

    /**
     * match
     */
    String MTH_INGRESS_PORT = "standard_metadata.ingress_port";
    String MTH_ETH_TYPE = "ethernet.ether_type";
    String MTH_ETH_SRC = "ethernet.src_addr";
    String MTH_ETH_DST = "ethernet.dst_addr";
    String MTH_IP_PROTO = "ipv4.protocol";
    String MTH_IP_SRC = "ipv4.src_addr";
    String MTH_IP_DST = "ipv4.dst_addr";
    String MTH_TCP_SRC = "tcp.src_port";
    String MTH_TCP_DST = "tcp.dst_port";
    String MTH_UDP_SRC = "udp.src_port";
    String MTH_UDP_DST = "udp.dst_port";

    /**
     * match ternary mask
     */
    String MASK_8_BITS = "0xff";
    String MASK_16_BITS = "0xffff";
    String MASK_48_BITS = "0xffffffffffff";
    String MASK_32_BITS = "0xffffffff";

    /**
     * action
     */
    String ACT_DENY = "ingress::deny";
    String ACT_ALLOW = "ingress::allow";
    String ACT_DROP = "ingress::drop";
    String ACT_FWD = "ingress::fwd";

    String ACT_PARAM_PORT = "port";

}
