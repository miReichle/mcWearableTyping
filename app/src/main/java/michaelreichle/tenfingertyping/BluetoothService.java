package michaelreichle.tenfingertyping;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT32;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

/**
 * By michaelreichle on 14.04.2018 at 22:23.
 */
public class BluetoothService extends Service {

    private final static String TAG = "service";
    private final IBinder binder = new BluetoothBinder();

    public class BluetoothBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    public static final String RESULT_DATA = "result";
    public static final String RESULT_FREQUENCY = "frequency";
    public static final String RESULT_MONITOR_COUNT = "monitor";

    private int connectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private BluetoothGatt gatt;
    private boolean discoveredServices = false;
    private boolean isVibrating = false;

    public static final String DEVICE_ARG = "device";

    public final static String ACTION_GATT_CONNECTED =
            "com.tenfingertyping.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.tenfingertyping.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.tenfingertyping.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.tenfingertyping.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.tenfingertyping.bluetooth.le.EXTRA_DATA";

    public final static UUID WEARABLE_SERVICE        = UUID.fromString("713D0000-503E-4C75-BA94-3148F18D941E");
    public final static UUID MONITOR_COUNT_ID        = UUID.fromString("713D0001-503E-4C75-BA94-3148F18D941E");
    public final static UUID MAXIMAL_FREQUENCE_ID    = UUID.fromString("713D0002-503E-4C75-BA94-3148F18D941E");
    public final static UUID SET_VIBRATION_ENGINE_ID = UUID.fromString("713D0003-503E-4C75-BA94-3148F18D941E");

    public void getMaxFrequency() { //TODO use
        if (connectionState == STATE_DISCONNECTED || !discoveredServices) {
            Log.w(TAG, "Can't get frequency yet.");
            return;
        }
        BluetoothGattCharacteristic frequencyCharacteristic = gatt.getService(WEARABLE_SERVICE).getCharacteristic(MAXIMAL_FREQUENCE_ID);
        gatt.readCharacteristic(frequencyCharacteristic);
    }

    public void getMonitorCount() {
        if (connectionState == STATE_DISCONNECTED || !discoveredServices) {
            Log.w(TAG, "Can't get monitor count yet.");
            return;
        }
        BluetoothGattCharacteristic monitorCharacteristic = gatt.getService(WEARABLE_SERVICE).getCharacteristic(MONITOR_COUNT_ID);
        gatt.readCharacteristic(monitorCharacteristic);
    }

    /**
     * @param values Length must be equal to monitor count.
     */
    public void setVibration(byte[] values) {
        if (connectionState == STATE_DISCONNECTED || !discoveredServices) {
            Log.w(TAG, "Can't set vibration yet.");
            return;
        }
        BluetoothGattCharacteristic vibrationCharacteristic = gatt.getService(WEARABLE_SERVICE).getCharacteristic(SET_VIBRATION_ENGINE_ID);
        vibrationCharacteristic.setValue(values);
        gatt.writeCharacteristic(vibrationCharacteristic);
    }

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                connectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" + gatt.discoverServices());
                Toast.makeText(BluetoothService.this, "Connected to device.", Toast.LENGTH_SHORT).show();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                connectionState = STATE_DISCONNECTED;
                discoveredServices = false;
                Log.d(TAG, "Disconnected from GATT server.");
                Toast.makeText(BluetoothService.this, "Disconnected from device. Attempting reconnect...", Toast.LENGTH_SHORT).show();
                broadcastUpdate(intentAction);
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Toast.makeText(BluetoothService.this, "Services discovered.", Toast.LENGTH_SHORT).show();
                discoveredServices = true;
            } else {
                Toast.makeText(BluetoothService.this, "Couldn't discover services.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
    };


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        if (MAXIMAL_FREQUENCE_ID.equals(characteristic.getUuid())) {
            final int frequency = characteristic.getIntValue(FORMAT_UINT8, 0);
            Log.d(TAG, String.format("Received maximal frequency: %d", frequency));
            intent.putExtra(EXTRA_DATA, frequency);
            intent.putExtra(RESULT_DATA, RESULT_FREQUENCY);
        } else if (MONITOR_COUNT_ID.equals(characteristic.getUuid())) {
            final int monitorCount = characteristic.getIntValue(FORMAT_UINT8, 0);
            Log.d(TAG, String.format("Received monitor count: %d", monitorCount));
            intent.putExtra(EXTRA_DATA, monitorCount);
            intent.putExtra(RESULT_DATA, RESULT_MONITOR_COUNT);
        } else if (SET_VIBRATION_ENGINE_ID.equals(characteristic.getUuid())) {
            isVibrating = characteristic.getIntValue(FORMAT_UINT8, 0) > 0 || characteristic.getIntValue(FORMAT_UINT32, 1) > 0;
        }
        sendBroadcast(intent);
    }

    public boolean isVibrating() {
        return isVibrating;
    }

    @Override
    public IBinder onBind(Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(DEVICE_ARG);
        if (device == null) {
            Log.w(TAG, "given device is null");
            Toast.makeText(this, "Started service with null device!", Toast.LENGTH_LONG).show();
        }
        gatt = device.connectGatt(this, true, gattCallback);
        return binder;
    }
}
