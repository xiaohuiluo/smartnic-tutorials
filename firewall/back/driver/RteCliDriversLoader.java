package cn.pcl.firewall.driver;

import org.onosproject.net.driver.DriverAdminService;
import org.onosproject.net.driver.DriverProvider;
import org.onosproject.net.driver.XmlDriverLoader;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

@Component(immediate = true)
public class RteCliDriversLoader{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private DriverProvider provider;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverAdminService driverAdminService;

    @Activate
    public void activate() {
        try(InputStream inputStream = FrameworkUtil.getBundle(RteCliDriversLoader.class).getResource("rtecli-drivers.xml").openStream()) {
            this.provider = (new XmlDriverLoader(this.getClass().getClassLoader(), this.driverAdminService)).loadDrivers(inputStream, this.driverAdminService);
            this.driverAdminService.registerProvider(this.provider);
        } catch (Exception e) {
            this.log.error("Unable to load {} driver definitions", "rtecli-drivers.xml", e);
        }

        log.info("load rtecli driver");
    }

    @Deactivate
    protected void deactivate() {

    }
}
