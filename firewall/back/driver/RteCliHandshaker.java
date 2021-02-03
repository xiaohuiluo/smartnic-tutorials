package cn.pcl.firewall.driver;

import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import java.util.concurrent.CompletableFuture;

public class RteCliHandshaker extends AbstractHandlerBehaviour implements DeviceHandshaker {

    public static final String NFP_NICS = "nfp_nics";

    @Override
    public boolean isReachable() {
        return true;
    }

    @Override
    public CompletableFuture<Boolean> probeReachability() {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.complete(true);
        return future;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public CompletableFuture<Boolean> probeAvailability() {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.complete(true);
        return future;
    }

    @Override
    public void roleChanged(MastershipRole newRole) {

    }

    @Override
    public MastershipRole getRole() {
        return MastershipRole.MASTER;
    }

    @Override
    public boolean connect() throws IllegalStateException {
        final DeviceId deviceId = data().deviceId();
        return true;
    }

    @Override
    public boolean hasConnection() {
        return true;
    }

    @Override
    public void disconnect() {

    }
}
