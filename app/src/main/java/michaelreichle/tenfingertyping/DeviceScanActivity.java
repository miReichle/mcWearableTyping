package michaelreichle.tenfingertyping;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import michaelreichle.tenfingertyping.DeviceListView.DeviceAdapter;
import michaelreichle.tenfingertyping.DeviceListView.DeviceHolder;

public class DeviceScanActivity extends AppCompatActivity {

    public static final int NONE = 0;
    public static final int SUCCESS = 1;
    public static final String DEVICE_EXTRA = "device_extra";

    private static final int BLE_NAME_IDS[] = {4};
    private final static int REQUEST_ENABLE_BT = 1;
    private static final String CURRENT_DEVICE_EXTRA = "cur_device_extra";

    // wearable specific values
    private final UUID SERVICE_UUID = UUID.fromString("713D0000-503E-4C75-BA94-3148F18D941E");

    private DeviceAdapter adapter;
    private FloatingActionButton fabSearch;
    private TextView selectedDeviceView;
    private ScanningCallback callback;
    private DeviceHolder currentDeviceHolder;

    private BluetoothAdapter bluetoothAdapter;
    private boolean scanning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);
        init();
        //adapter.add(new DeviceHolder(null, "test", "test"));
        //adapter.add(new DeviceHolder(null, "banane", "banane"));
    }

    private void init() {
        ListView deviceView = (ListView) findViewById(R.id.device_list_view);
        callback = new ScanningCallback();
        adapter = new DeviceAdapter(this, new ArrayList<DeviceHolder>());
        deviceView.setAdapter(adapter);
        deviceView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                adapter.setSelectedPosition(i);
                currentDeviceHolder = (DeviceHolder) adapter.getItem(i);
            }
        });
        selectedDeviceView = (TextView) findViewById(R.id.selectedDevice);
        Bundle b = getIntent().getExtras();
        if (b != null) {
            currentDeviceHolder = b.getParcelable(CURRENT_DEVICE_EXTRA);
            selectedDeviceView.setText((currentDeviceHolder != null) ? currentDeviceHolder.getName() : "none");
        }

        Button buttonCancel = (Button) findViewById(R.id.cancelButton);
        Button buttonConnect = (Button) findViewById(R.id.connectButton);
        fabSearch = (FloatingActionButton) findViewById(R.id.fabSearch);

        fabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scanning) {
                    scanDevices(false);
                } else {
                    scanDevices(true);
                }
            }
        });
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancel();
            }
        });
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
            }
        });
    }

    private void setScanning(boolean scanning) {
        this.scanning = scanning;
        if (scanning) {
            Toast.makeText(this, "scanning...", Toast.LENGTH_LONG).show();
            fabSearch.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_stop));
        } else {
            Toast.makeText(this, "stopped scanning.", Toast.LENGTH_SHORT).show();
            fabSearch.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_bluetooth_search));
        }
    }

    private void connect() {
        Intent intent = new Intent();
        if (currentDeviceHolder == null || !currentDeviceHolder.isSupported()) {
            setResult(NONE, intent);
        } else {
            intent.putExtra(DEVICE_EXTRA, currentDeviceHolder);
            setResult(RESULT_OK, intent);
        }
        finish();
    }

    private void cancel() {
        Intent intent = new Intent();
        setResult(NONE, intent);
        finish();
    }

    // TODO: check if device can be found
    private void initBLE() {
        // TODO: check for services
        if (bluetoothAdapter == null) {
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }


    private void scanDevices(final boolean enable) {
        setScanning(enable);
        if (enable) {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Scan failed.", Toast.LENGTH_SHORT).show();
                return;
            }
            ArrayList<ScanFilter> filters = new ArrayList<>();
            for (final int BLE_NAME_ID : BLE_NAME_IDS) {
                // only search for wearables
                ScanFilter filter = new ScanFilter.Builder()
                        //.setDeviceName("TECO WEARABLE " + BLE_NAME_ID) // filters not working
                        //.setServiceUuid(new ParcelUuid(SERVICE_UUID))
                        .build();
                filters.add(filter);
            }

            ScanSettings settings = new ScanSettings.Builder()
                    .setReportDelay(0)
                    .build();
            bluetoothAdapter.getBluetoothLeScanner().startScan(filters, settings, callback);
        } else {
            bluetoothAdapter.getBluetoothLeScanner().stopScan(callback);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            cancel();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initBLE();
        /*String name;
        if (currentDevice == null) {
            name = "none";
        } else if (currentDevice.getName() == null) {
            name = DeviceHolder.DEFAULT_DEVICE_NAME;
        } else {
            name = currentDevice.getName();
        }*/
        selectedDeviceView.setText((currentDeviceHolder != null) ?  currentDeviceHolder.getName() : "none");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (scanning) {
            scanDevices(false);
        }
        adapter.clear();
    }

    private class ScanningCallback extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (!scanning) {
                Log.d(MainActivity.BLE_LOG, "Scan result while not scanning.");
                return;
            }
            handleResult(callbackType, result);
            DeviceScanActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            if (!scanning) {
                Log.d(MainActivity.BLE_LOG, "Scan result while not scanning.");
                return;
            }
            for (ScanResult result : results) {
                handleResult(1, result);
            }
            DeviceScanActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
            DeviceScanActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }

        private void handleResult(int callbackType, ScanResult result) {
            BluetoothDevice bleDevice = result.getDevice();
            DeviceHolder device = new DeviceHolder(bleDevice, result.getScanRecord().getDeviceName(), bleDevice.getAddress());
            Log.d(MainActivity.BLE_LOG, "uuids: " + device.toString());
            Log.d(MainActivity.BLE_LOG, "result:" + result.toString());
            Log.d(MainActivity.BLE_LOG, "uuid: " + Arrays.toString(result.getDevice().getUuids()));
            if (!adapter.contains(device)) {
                adapter.add(device);
                Log.d(MainActivity.BLE_LOG, "device added.");
            } else {
                Log.d(MainActivity.BLE_LOG, "already added device found.");
            }
        }

        @Override
        public void onScanFailed(final int errorCode) {
            DeviceScanActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    setScanning(false);
                    Toast.makeText(DeviceScanActivity.this, "Scan failed with error code " + errorCode + ".", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public static void start(Activity activity, int requestCode, DeviceHolder device) {
        Intent intent = new Intent(activity, DeviceScanActivity.class);
        intent.putExtra(CURRENT_DEVICE_EXTRA, device);
        activity.startActivityForResult(intent, requestCode);
    }
}
