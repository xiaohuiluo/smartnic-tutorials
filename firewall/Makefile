APP_OAR = firewall-app/target/firewall-app-1.0-SNAPSHOT.oar
APP_PATH= firewall-app/
OCI=127.0.0.1

p4:
	cd p4 && make all

onos-run:
	$(info ************ STARTING ONOS ************)
	sh $(APP_PATH)/start_onos.sh

onos-run-debug:
	$(info ************ STARTING ONOS with DEBUG ************)
	sh $(APP_PATH)/start_onos.sh debug

onos-cli:
	onos ${OCI}

netcfg-firewall:
	$(info ************ PUSHING NETCFG MP TO ONOS ************)
	onos-netcfg ${OCI} ${APP_PATH}/src/main/resources/nfp_nics.json

app-build:
	$(info ************ BUILDING ONOS APP ************)
	cd $(APP_PATH) && mvn clean install

$(APP_OAR):
	$(error Missing app binary, run 'make app-build' first)

app-reload: $(APP_OAR)
	$(info ************ RELOADING ONOS APP ************)
	onos-app ${OCI} reinstall! ${APP_OAR}

