OUT_DIR=./build
P4C_BM_EXE=p4c-bm2-ss
BMV2_PP_FLAGS=-DTARGET_BMV2

all: build

build: firewall.p4
	$(info ************ COMPILING P4 PROGRAM ************)
	mkdir -p $(OUT_DIR)
	$(P4C_BM_EXE) --arch v1model -o $(OUT_DIR)/bmv2.json \
		$(BMV2_PP_FLAGS) $(OTHER_PP_FLAGS) \
		--p4runtime-files $(OUT_DIR)/p4info.txt \
		firewall.p4

	#cp ./build/* ../onos-p4-qos/src/main/resources/bmv2/

clean:
	#rm -f ../onos-p4-qos/src/main/resources/bmv2/bmv2.json
	#rm -f ../onos-p4-qos/src/main/resources/bmv2/p4info.txt
	rm -rf ./build
