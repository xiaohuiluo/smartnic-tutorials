package cn.pcl.firewall.driver;

import org.onosproject.net.driver.AbstractDriverLoader;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class RteCliDriversLoader extends AbstractDriverLoader {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public RteCliDriversLoader() {
        super("/rtecli-drivers.xml");
    }

    @Override
    public void activate() {
        super.activate();
        log.info("load rtecli driver");
    }
}
