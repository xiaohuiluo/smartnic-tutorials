# smartnic-tutorials
tutorials of SmartNIC



rtecli使用

```bash
xiaoh@DESKTOP-7DSOC9P MINGW64 /d/Softwares/NFP_SDK_6.1.0-preview/p4/bin
$ ./rtecli.exe --rte-port 20206 --rte-host 192.168.67.143 -h
usage: rtecli.exe [-h] [--version] [-r RTE_HOST] [-p RTE_PORT] [-n]
                  [-t {thrift,grpc}] [-d DEVICE_ID] [-j]

                  {design-load,version,status,design-unload,config-reload,counters,tables,registers,meters,traffic-classes,digests,parser_value_sets,ports,multicast,debugctl}
                  ...

RTE command line interface

positional arguments:
  {design-load,version,status,design-unload,config-reload,counters,tables,registers,meters,traffic-classes,digests,parser_value_sets,ports,multicast,debugctl}
                        rte client commands
    design-load         load a pif design
    version             get the remote version number
    status              get the remote load status
    design-unload       unload a pif design
    config-reload       reload a user config
    counters            counter commands
    tables              table commands
    registers           register commands
    meters              meter commands
    traffic-classes     traffic class commands
    digests             digest commands
    parser_value_sets   parser_value_sets commands
    ports               ports commands
    multicast           multicast commands
    debugctl            ==SUPPRESS==

optional arguments:
  -h, --help            show this help message and exit
  --version             show program's version number and exit
  -r RTE_HOST, --rte-host RTE_HOST
                        rte host, default localhost
  -p RTE_PORT, --rte-port RTE_PORT
                        rte port, default 20206
  -n, --rte-no-zlib     don't use zlib for buffer transport
  -t {thrift,grpc}, --rpc {thrift,grpc}
                        set the rpc connection method to the rte
  -d DEVICE_ID, --device-id DEVICE_ID
                        device id to connect to (used by grpc)
  -j, --json            return results as json

Copyright (C) 2016, 2017 Netronome Systems, Inc. All rights reserved.

xiaoh@DESKTOP-7DSOC9P MINGW64 /d/Softwares/NFP_SDK_6.1.0-preview/p4/bin
$ ./rtecli.exe --rte-port 20206 --rte-host 192.168.67.143 status
{'frontend_build_date': '2021/01/12 09:00:21',
 'frontend_source': 'firewall.p4',
 'frontend_version': '',
 'is_loaded': True,
 'uptime': 2083,
 'uuid': '9a081700-54b4-11eb-bf57-1856808f7f97'}

xiaoh@DESKTOP-7DSOC9P MINGW64 /d/Softwares/NFP_SDK_6.1.0-preview/p4/bin
$ ./rtecli.exe --rte-port 20206 --rte-host 192.168.67.143 tables edit -t ingress::t_fwd -r fwd1to4 -m '{"standard_metadata.ingress_port": { "value": "p0" }}' -a '{"data": { "port": { "value": "p4" }}, "type": "ingress::fwd"}'
'Rule fwd1to4 in table ingress::t_fwd edited'

xiaoh@DESKTOP-7DSOC9P MINGW64 /d/Softwares/NFP_SDK_6.1.0-preview/p4/bin
$ ./rtecli.exe --rte-port 20206 --rte-host 192.168.67.143 tables edit -t ingress::t_fwd -r fwd4to0 -m '{"standard_metadata.ingress_port": { "value": "p4" }}' -a '{"data": { "port": { "value": "p0" }}, "type": "ingress::fwd"}'
'Rule fwd4to0 in table ingress::t_fwd edited'

xiaoh@DESKTOP-7DSOC9P MINGW64 /d/Softwares/NFP_SDK_6.1.0-preview/p4/bin
$ ./rtecli.exe --rte-port 20206 --rte-host 192.168.67.143 tables --table-name ingress::t_fwd list-rules
[{'actions': '{  "type" : "ingress::drop",  "data" : {  } }',
  'default_rule': True,
  'match': '{  }',
  'priority': 0,
  'rule_name': 'application_default',
  'timeout_seconds': 0},
 {'actions': '{  "type" : "ingress::fwd",  "data" : { "port" : { "value" : "p4" } } }',
  'default_rule': False,
  'match': '{ "standard_metadata.ingress_port" : {  "value" : "p0", "mask" : "" } }',
  'priority': 0,
  'rule_name': 'fwd1to4',
  'timeout_seconds': 0},
 {'actions': '{  "type" : "ingress::fwd",  "data" : { "port" : { "value" : "p0" } } }',
  'default_rule': False,
  'match': '{ "standard_metadata.ingress_port" : {  "value" : "p4", "mask" : "" } }',
  'priority': 0,
  'rule_name': 'fwd4to0',
  'timeout_seconds': 0}]

xiaoh@DESKTOP-7DSOC9P MINGW64 /d/Softwares/NFP_SDK_6.1.0-preview/p4/bin
$ ./rtecli.exe --rte-port 20206 --rte-host 192.168.67.143 ports list
[{'id': 768, 'info': 'host virtual function 0', 'token': 'v0.0'},
 {'id': 769, 'info': 'host virtual function 1', 'token': 'v0.1'},
 {'id': 770, 'info': 'host virtual function 2', 'token': 'v0.2'},
 {'id': 771, 'info': 'host virtual function 3', 'token': 'v0.3'},
 {'id': 772, 'info': 'host virtual function 4', 'token': 'v0.4'},
 {'id': 773, 'info': 'host virtual function 5', 'token': 'v0.5'},
 {'id': 774, 'info': 'host virtual function 6', 'token': 'v0.6'},
 {'id': 775, 'info': 'host virtual function 7', 'token': 'v0.7'},
 {'id': 776, 'info': 'host virtual function 8', 'token': 'v0.8'},
 {'id': 777, 'info': 'host virtual function 9', 'token': 'v0.9'},
 {'id': 778, 'info': 'host virtual function 10', 'token': 'v0.10'},
 {'id': 779, 'info': 'host virtual function 11', 'token': 'v0.11'},
 {'id': 780, 'info': 'host virtual function 12', 'token': 'v0.12'},
 {'id': 781, 'info': 'host virtual function 13', 'token': 'v0.13'},
 {'id': 782, 'info': 'host virtual function 14', 'token': 'v0.14'},
 {'id': 783, 'info': 'host virtual function 15', 'token': 'v0.15'},
 {'id': 784, 'info': 'host virtual function 16', 'token': 'v0.16'},
 {'id': 785, 'info': 'host virtual function 17', 'token': 'v0.17'},
 {'id': 786, 'info': 'host virtual function 18', 'token': 'v0.18'},
 {'id': 787, 'info': 'host virtual function 19', 'token': 'v0.19'},
 {'id': 788, 'info': 'host virtual function 20', 'token': 'v0.20'},
 {'id': 789, 'info': 'host virtual function 21', 'token': 'v0.21'},
 {'id': 790, 'info': 'host virtual function 22', 'token': 'v0.22'},
 {'id': 791, 'info': 'host virtual function 23', 'token': 'v0.23'},
 {'id': 792, 'info': 'host virtual function 24', 'token': 'v0.24'},
 {'id': 793, 'info': 'host virtual function 25', 'token': 'v0.25'},
 {'id': 794, 'info': 'host virtual function 26', 'token': 'v0.26'},
 {'id': 795, 'info': 'host virtual function 27', 'token': 'v0.27'},
 {'id': 796, 'info': 'host virtual function 28', 'token': 'v0.28'},
 {'id': 797, 'info': 'host virtual function 29', 'token': 'v0.29'},
 {'id': 798, 'info': 'host virtual function 30', 'token': 'v0.30'},
 {'id': 799, 'info': 'host virtual function 31', 'token': 'v0.31'},
 {'id': 800, 'info': 'host virtual function 32', 'token': 'v0.32'},
 {'id': 801, 'info': 'host virtual function 33', 'token': 'v0.33'},
 {'id': 802, 'info': 'host virtual function 34', 'token': 'v0.34'},
 {'id': 803, 'info': 'host virtual function 35', 'token': 'v0.35'},
 {'id': 804, 'info': 'host virtual function 36', 'token': 'v0.36'},
 {'id': 805, 'info': 'host virtual function 37', 'token': 'v0.37'},
 {'id': 806, 'info': 'host virtual function 38', 'token': 'v0.38'},
 {'id': 807, 'info': 'host virtual function 39', 'token': 'v0.39'},
 {'id': 808, 'info': 'host virtual function 40', 'token': 'v0.40'},
 {'id': 809, 'info': 'host virtual function 41', 'token': 'v0.41'},
 {'id': 810, 'info': 'host virtual function 42', 'token': 'v0.42'},
 {'id': 811, 'info': 'host virtual function 43', 'token': 'v0.43'},
 {'id': 812, 'info': 'host virtual function 44', 'token': 'v0.44'},
 {'id': 813, 'info': 'host virtual function 45', 'token': 'v0.45'},
 {'id': 814, 'info': 'host virtual function 46', 'token': 'v0.46'},
 {'id': 815, 'info': 'host virtual function 47', 'token': 'v0.47'},
 {'id': 816, 'info': 'host virtual function 48', 'token': 'v0.48'},
 {'id': 817, 'info': 'host virtual function 49', 'token': 'v0.49'},
 {'id': 818, 'info': 'host virtual function 50', 'token': 'v0.50'},
 {'id': 819, 'info': 'host virtual function 51', 'token': 'v0.51'},
 {'id': 820, 'info': 'host virtual function 52', 'token': 'v0.52'},
 {'id': 821, 'info': 'host virtual function 53', 'token': 'v0.53'},
 {'id': 822, 'info': 'host virtual function 54', 'token': 'v0.54'},
 {'id': 823, 'info': 'host virtual function 55', 'token': 'v0.55'},
 {'id': 824, 'info': 'host virtual function 56', 'token': 'v0.56'},
 {'id': 825, 'info': 'host virtual function 57', 'token': 'v0.57'},
 {'id': 826, 'info': 'host virtual function 58', 'token': 'v0.58'},
 {'id': 827, 'info': 'host virtual function 59', 'token': 'v0.59'},
 {'id': 828, 'info': 'host virtual function 60', 'token': 'v0.60'},
 {'id': 829, 'info': 'host virtual function 61', 'token': 'v0.61'},
 {'id': 830, 'info': 'host virtual function 62', 'token': 'v0.62'},
 {'id': 831, 'info': 'host virtual function 63', 'token': 'v0.63'},
 {'id': 0, 'info': 'NBI0 40G', 'token': 'p0'},
 {'id': 4, 'info': 'NBI0 40G', 'token': 'p4'}]

$ ./rtecli.exe --rte-port 20206 --rte-host 192.168.67.143 tables -h
usage: rtecli.exe tables [-h] [-t TBL_NAME] [-i TBL_ID] [-r RULE_NAME] [-d]
                         [-m MATCH] [-a ACTION] [-p PRIORITY] [-o TIMEOUT]
                         {list,list-rules,add,edit,delete}

positional arguments:
  {list,list-rules,add,edit,delete}
                        table command

optional arguments:
  -h, --help            show this help message and exit
  -t TBL_NAME, --table-name TBL_NAME
                        name of command target table
  -i TBL_ID, --table-id TBL_ID
                        name of command target table id
  -r RULE_NAME, --rule RULE_NAME
                        name of command target rule
  -d, --default-rule    flag to set whether rule is target table default rule
  -m MATCH, --match MATCH
                        matchfields in JSON format for entry commands
  -a ACTION, --action ACTION
                        actions in JSON format for entry commands
  -p PRIORITY, --priority PRIORITY
                        optional priority for rule
  -o TIMEOUT, --timeout TIMEOUT
                        optional timeout in seconds for rule

xiaoh@DESKTOP-7DSOC9P MINGW64 /d/Softwares/NFP_SDK_6.1.0-preview/p4/bin
$ ./rtecli.exe --rte-port 20206 --rte-host 192.168.67.143 tables delete -t ingress::t_fwd -r fwd4to0 -m '{"standard_metadata.ingress_port": { "value": "p4" }}' -a '{"data": { "port": { "value": "p0" }}, "type": "ingress::fwd"}'
'Rule fwd4to0 in table ingress::t_fwd deleted'

xiaoh@DESKTOP-7DSOC9P MINGW64 /d/Softwares/NFP_SDK_6.1.0-preview/p4/bin
$ ./rtecli.exe --rte-port 20206 --rte-host 192.168.67.143 tables delete -t ingress::t_fwd -r fwd1to4 -m '{"standard_metadata.ingress_port": { "value": "p0" }}' -a '{"data": { "port": { "value": "p4" }}, "type": "ingress::fwd"}'
'Rule fwd1to4 in table ingress::t_fwd deleted'

xiaoh@DESKTOP-7DSOC9P MINGW64 /d/Softwares/NFP_SDK_6.1.0-preview/p4/bin
$ ./rtecli.exe --rte-port 20206 --rte-host 192.168.67.143 tables add -t ingress::t_fwd -r fwdv1tov2 -m '{"standard_metadata.ingress_port": { "value": "768" }}' -a '{"data": { "port": { "value": "769" }}, "type": "ingress::fwd"}'
'Rule fwdv1tov2 added to table ingress::t_fwd'

xiaoh@DESKTOP-7DSOC9P MINGW64 /d/Softwares/NFP_SDK_6.1.0-preview/p4/bin
$ ./rtecli.exe --rte-port 20206 --rte-host 192.168.67.143 tables add -t ingress::t_fwd -r fwdv2tov1 -m '{"standard_metadata.ingress_port": { "value": "769" }}' -a '{"data": { "port": { "value": "768" }}, "type": "ingress::fwd"}'
'Rule fwdv2tov1 added to table ingress::t_fwd'


xiaoh@DESKTOP-7DSOC9P MINGW64 /d/Softwares/NFP_SDK_6.1.0-preview/p4/bin
$ ./rtecli.exe --rte-port 20206 --rte-host 192.168.67.143 tables delete -t ingress::t_fwd -r fwdv1tov2 -m '{"standard_metadata.ingress_port": { "value": "768" }}' -a '{"data": { "port": { "value": "769" }}, "type": "ingress::fwd"}'
'Rule fwdv1tov2 in table ingress::t_fwd deleted'

xiaoh@DESKTOP-7DSOC9P MINGW64 /d/Softwares/NFP_SDK_6.1.0-preview/p4/bin
$ ./rtecli.exe --rte-port 20206 --rte-host 192.168.67.143 tables delete -t ingress::t_fwd -r fwdv2tov1 -m '{"standard_metadata.ingress_port": { "value": "769" }}' -a '{"data": { "port": { "value": "768" }}, "type": "ingress::fwd"}'
'Rule fwdv2tov1 in table ingress::t_fwd deleted'


xiaoh@DESKTOP-7DSOC9P MINGW64 /d/Softwares/NFP_SDK_6.1.0-preview/p4/bin
$ ./rtecli.exe --rte-port 20206 --rte-host 192.168.67.143 counters list
[{'count': 1001,
  'id': 0,
  'name': 't_acl_counter_bytes',
  'table': 'ingress::t_acl',
  'tableid': 1,
  'type': 'P4CounterType.Direct',
  'width': 64},
 {'count': 3,
  'id': 1,
  'name': 't_fwd_counter_packets',
  'table': 'ingress::t_fwd',
  'tableid': 0,
  'type': 'P4CounterType.Direct',
  'width': 64},
 {'count': 1001,
  'id': 2,
  'name': 't_acl_counter_packets',
  'table': 'ingress::t_acl',
  'tableid': 1,
  'type': 'P4CounterType.Direct',
  'width': 64},
 {'count': 3,
  'id': 3,
  'name': 't_fwd_counter_bytes',
  'table': 'ingress::t_fwd',
  'tableid': 0,
  'type': 'P4CounterType.Direct',
  'width': 64}]

```

