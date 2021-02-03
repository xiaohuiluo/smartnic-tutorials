package cn.pcl.firewall.provider;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class NfpRteCliProvider implements DeviceProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String SCHEME = "rtecli";

    public static final String PROVIDER_ID = "cn.pcl.provider.nfp.nic";


    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceProviderRegistry deviceProviderRegistry;

    private DeviceProviderService deviceProviderService;

    @Activate
    public void activate() {
        deviceProviderService = deviceProviderRegistry.register(this);
        log.info("Start NfpRteCliProvider");
    }

    @Deactivate
    public void deactivate() {
        deviceProviderRegistry.unregister(this);
        deviceProviderService = null;
        log.info("Stop NfpRteCliProvider");
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {

    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {

    }

    @Override
    public boolean isReachable(DeviceId deviceId) {

        Device device = deviceService.getDevice(deviceId);
        if (device.is(DeviceHandshaker.class)) {
            DeviceHandshaker handshaker = device.as(DeviceHandshaker.class);

            log.info("reach with driver isReachable={}", handshaker.isReachable());
            return handshaker.isReachable();
        }

        return false;
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber, boolean enable) {
        if (isReachable(deviceId)) {

        }
    }

    @Override
    public ProviderId id() {
        return new ProviderId(SCHEME, PROVIDER_ID);
    }
}
