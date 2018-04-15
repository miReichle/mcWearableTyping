package michaelreichle.tenfingertyping.DeviceListView;

import android.bluetooth.BluetoothDevice;

/**
 * By michaelreichle on 14.04.2018 at 16:50.
 */

public class DeviceHolder {
    private BluetoothDevice device;
    private String name;
    private String mac;
    public final static String DEFAULT_DEVICE_NAME = "unnamed";

    public DeviceHolder(BluetoothDevice device, String name, String mac) {
        this.device = device;
        this.name = name;
        this.mac = mac;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public String getName() {
        return (name == null) ? DEFAULT_DEVICE_NAME : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof DeviceHolder)) {
            return false;
        }

        DeviceHolder holder = (DeviceHolder) obj;
        return holder.getName().equals(this.getName()) && holder.getMac().equals(this.getMac());
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + getName().hashCode();
        result = 31 * result + getMac().hashCode();
        return result;
    }
}
