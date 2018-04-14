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
import android.os.ParcelUuid;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import michaelreichle.tenfingertyping.DeviceListView.DeviceAdapter;
import michaelreichle.tenfingertyping.DeviceListView.DeviceHolder;

public class DeviceScanActivity extends AppCompatActivity {

    public static final int NONE = 0;
    public static final int SUCCESS = 1;
    public static final String DEVICE_EXTRA = "device_extra";


    private static final long SCAN_PERIOD = 10000;
    private static final int BLE_NAME_IDS[] = {4};
    private final static int REQUEST_ENABLE_BT = 1;
    private static final String CURRENT_DEVICE_EXTRA = "device_extra";

    // wearable specific values
    private final UUID SERVICE_UUID = new UUID(0x713D0000503E4C75L, 0xBA943148F18D941EL);

    private ListView deviceView;
    private DeviceAdapter adapter;
    private FloatingActionButton fabSearch;

    private BluetoothDevice currentDevice;

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
        deviceView = (ListView) findViewById(R.id.device_list_view);

        adapter = new DeviceAdapter(this, new ArrayList<DeviceHolder>());
        deviceView.setAdapter(adapter);
        deviceView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                adapter.setSelectedPosition(i);
                DeviceHolder holder = (DeviceHolder) adapter.getItem(i);
                currentDevice = holder.getDevice();
                Toast.makeText(DeviceScanActivity.this, "Selected " + holder.getName() + ".", Toast.LENGTH_SHORT).show();
            }
        });

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
        intent.putExtra(DEVICE_EXTRA, currentDevice);
        setResult((currentDevice == null) ? NONE : SUCCESS, intent);
        finish();
    }

    private void cancel() {
        Intent intent = new Intent();
        BluetoothDevice res = null;
        intent.putExtra(DEVICE_EXTRA, res);
        setResult(NONE, intent);
        finish();
    }

    // TODO: check if device can be found

    private void initBLE() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void scanDevices(final boolean enable) {
        setScanning(enable);
        if (enable) {
            initBLE();
            ArrayList<ScanFilter> filters = new ArrayList<>();
            for (final int BLE_NAME_ID : BLE_NAME_IDS) {
                // only search for wearables
                ScanFilter filter = new ScanFilter.Builder()
                        .setDeviceName("TECO WEARABLE " + BLE_NAME_ID)
                        .setServiceUuid(new ParcelUuid(SERVICE_UUID))
                        .build();
                filters.add(filter);
            }
            ScanSettings settings = new ScanSettings.Builder()
                    .setReportDelay(0)
                    .build();
            bluetoothAdapter.getBluetoothLeScanner().startScan(filters, settings, new ScanningCallback());
        } else {
            bluetoothAdapter.getBluetoothLeScanner().stopScan(new ScanningCallback());
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
            Toast.makeText(DeviceScanActivity.this, "result found", Toast.LENGTH_SHORT).show();
            BluetoothDevice bleDevice = result.getDevice();
            DeviceHolder device = new DeviceHolder(bleDevice, bleDevice.getName(), bleDevice.getAddress());
            if (!adapter.contains(device)) {
                adapter.add(device);
                Log.d(MainActivity.BLE_LOG, "device added.");
            } else {
                Log.d(MainActivity.BLE_LOG, "already added device found.");
            }
            DeviceScanActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                BluetoothDevice bleDevice = result.getDevice();
                DeviceHolder device = new DeviceHolder(bleDevice, bleDevice.getName(), bleDevice.getAddress());
                if (!adapter.contains(device)) {
                    adapter.add(device);
                    Log.d(MainActivity.BLE_LOG,  "device added.");
                } else {
                    Log.d(MainActivity.BLE_LOG,  "already added device found.");
                }
                Toast.makeText(DeviceScanActivity.this, "batch result found", Toast.LENGTH_SHORT).show();
            }
            DeviceScanActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onScanFailed(int errorCode) {
            DeviceScanActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    setScanning(false);
                    Toast.makeText(DeviceScanActivity.this, "Scan failed", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public static void start(Activity activity, int requestCode, BluetoothDevice device) {
        Intent intent = new Intent(activity, DeviceScanActivity.class);
        intent.putExtra(CURRENT_DEVICE_EXTRA, device);
        activity.startActivityForResult(intent, requestCode);
    }
}
