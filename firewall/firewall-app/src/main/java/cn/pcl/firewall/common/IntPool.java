package cn.pcl.firewall.common;

import java.util.Arrays;

public class IntPool {
    private final int startInt;
    private final int endInt;

    private final int length;

    private boolean[] poolFlags;

    private final Object lock = new Object();

    public IntPool(int startInt, int endInt) {
        if (endInt < startInt) {
            throw new IllegalArgumentException("endInt must >= startInt : startInt=" + startInt + ",endInt=" + endInt);
        }

        this.startInt = startInt;
        this.endInt = endInt;
        this.length = endInt - startInt;
        poolFlags = new boolean[endInt - startInt];
    }

    public int acquire() {
        synchronized (lock) {
            for (int i = 0; i < length; i++) {
                if (!poolFlags[i]) {
                    poolFlags[i] = true;
                    return startInt + i;
                }
            }
        }

        throw new RuntimeException("port pool is full in used");
    }

    public void markStatus(int port, boolean status) {
        if (port < startInt || port > endInt) {
            return;
        }

        int index = port - startInt;
        synchronized (lock) {
            poolFlags[index] = status;
        }
    }

    public boolean getStatus(int port) {
        if (port < startInt || port > endInt) {
            throw new IllegalArgumentException("port=" + port + " is not in port poll");
        }

        int index = port - startInt;
        return poolFlags[index];
    }

    public boolean contains(int port) {
        return port < startInt || port > endInt;
    }

    public void release(int port) {
        if (port < startInt || port > endInt) {
            //throw new IllegalArgumentException("port=" + port + " is not in port poll");
            return;
        }

        int index = port - startInt;
        synchronized (lock) {
            poolFlags[index] = false;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("IntPool{");
        sb.append("startInt=").append(startInt);
        sb.append(", endInt=").append(endInt);
        sb.append(", length=").append(length);
        sb.append(", poolFlags=").append(Arrays.toString(poolFlags));
        sb.append('}');
        return sb.toString();
    }

}
