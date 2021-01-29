package cn.pcl.firewall.driver;

import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;

import java.util.List;

public class RteCliDeviceDescriptionDiscovery implements DeviceDescriptionDiscovery {
    @Override
    public DeviceDescription discoverDeviceDetails() {
        return null;
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        return null;
    }

    @Override
    public DriverHandler handler() {
        return null;
    }

    @Override
    public void setHandler(DriverHandler handler) {

    }

    @Override
    public DriverData data() {
        return null;
    }

    @Override
    public void setData(DriverData data) {

    }
}
