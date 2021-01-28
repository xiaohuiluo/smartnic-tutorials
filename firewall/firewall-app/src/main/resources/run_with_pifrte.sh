sudo modprobe -r -v nfp && sudo modprobe nfp nfp_pf_netdev=0 nfp_dev_cpp=1
/opt/nfp_pif/bin/pif_rte -n 0 -p 20206 -I -z -s /opt/nfp_pif/scripts/pif_ctl_nfd.sh -d ./pif_design.json -f ./firewall.nffw
# -c ./firewall.p4cfg
