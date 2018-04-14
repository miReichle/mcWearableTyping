package michaelreichle.tenfingertyping;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import static michaelreichle.tenfingertyping.R.id.floatingActionButton;

public class MainActivity extends AppCompatActivity {

    public final static String RUNNABLE_LOG = "runnable";
    public final static String BLE_LOG = "ble";

    private final static int DEVICE_SCAN_REQUEST = 5;

    private SeekBar cpmBar;
    private FloatingActionButton playFab;
    private TextView cpmView;
    private TextView charView;
    private TextView leftView;
    private TextView rightView;
    private TextView textView;

    private String text;
    private String currentWord = "";
    private int currentCharIndex = 0;

    private int cpm;
    private int MAX_CPM = 510; // TODO: compute by maximum vibration frequency
    private int MIN_CPM = 10;
    private int maxAllowedCpm = -1;

    private boolean isRunning = false;
    BluetoothDevice device = null;

    final Handler handler = new Handler();
    final Runnable runnable = new Runnable() {
        public void run() {
            Log.d(RUNNABLE_LOG,"Handler is working");
            if (text.isEmpty() || !isRunning){
                handler.removeCallbacks(this);
                Log.d(RUNNABLE_LOG,"stopped");
                if (text.isEmpty()) {
                    reset();
                }
            } else {
                char currentChar = text.charAt(0);
                text = text.substring(1);
                if (currentChar == ' ') {
                    int i = text.indexOf(' ');
                    if (i == -1) i = text.length();
                    currentWord = text.substring(0, i);
                    currentCharIndex = 0;
                    leftView.setText("");
                    charView.setText("");
                    rightView.setText("");
                } else {
                    leftView.setText(currentWord.substring(0, currentCharIndex));
                    charView.setText(String.valueOf(currentChar));
                    rightView.setText(currentWord.substring(currentCharIndex + 1));
                    currentCharIndex++;
                }
                textView.setText(text);
                notifyWearable(currentChar);

                handler.postDelayed(this, getDelay());
            }
        }
    };

    // Bluetooth Service
    private BluetoothService bluetoothService;
    private boolean connected = false;
    private boolean discovered = false;
    boolean bound = false;
    private int monitorCount = -1;
    /** Defines callbacks for bluetoothService binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to BluetoothService, cast the IBinder and get LocalService instance
            BluetoothService.BluetoothBinder binder = (BluetoothService.BluetoothBinder) service;
            bluetoothService = binder.getService();
            bound = true;
            // TODO: check frequency, set maxAllowedCpm
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            maxAllowedCpm = -1;
            bound = false;
        }
    };

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
            } else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                discovered = false;
                monitorCount = -1;
                maxAllowedCpm = -1;
            } else if (BluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                discovered = true;
            } else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (BluetoothService.RESULT_MONITOR_COUNT.equals(intent.getStringExtra(BluetoothService.RESULT_DATA))) {
                    monitorCount = intent.getIntExtra(BluetoothService.EXTRA_DATA, -1);
                } else if (BluetoothService.RESULT_FREQUENCY.equals(intent.getStringExtra(BluetoothService.RESULT_DATA))) {
                    int frequency = intent.getIntExtra(BluetoothService.EXTRA_DATA, -1);
                    maxAllowedCpm = frequency * 60;
                }
            }
        }
    };




    private void reset() {
        String l = getResources().getString(R.string.left);
        String r = getResources().getString(R.string.right);
        String c = getResources().getString(R.string.startingCharacter);
        String t = getResources().getString(R.string.typeText);
        leftView.setText(l);
        rightView.setText(r);
        charView.setText(c);
        textView.setText(t);
        currentWord = "";
        stopWriting();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(gattUpdateReceiver, new IntentFilter());

        if (device != null) {
            bindBluetoothService();
        }
    }

    private void bindBluetoothService() {
        if (connection != null) unbindService(connection);
        Intent intent = new Intent(this, BluetoothService.class);
        intent.putExtra(BluetoothService.DEVICE_ARG, device);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(gattUpdateReceiver);
        unbindService(connection);
        bound = false;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.connect:
                showConnectScreen();
                return true;
            case R.id.text_options:
                showTextOptions();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showTextOptions() {
        // TODO: set options
    }

    private void showConnectScreen() {
        DeviceScanActivity.start(this, DEVICE_SCAN_REQUEST, device);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DEVICE_SCAN_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    device = data.getParcelableExtra(DeviceScanActivity.DEVICE_EXTRA);
                    bindBluetoothService();
                    Toast.makeText(this, "Selected " + device.getName() + ".", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Did not connect to a BLE device.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }



    private void init() {
        cpmBar= (SeekBar) findViewById(R.id.seekBar);
        textView = (TextView) findViewById(R.id.text);
        playFab = (FloatingActionButton) findViewById(floatingActionButton);
        cpmView = (TextView) findViewById(R.id.cpm);
        charView = (TextView) findViewById(R.id.currentChar);
        rightView = (TextView) findViewById(R.id.textRightView);
        leftView = (TextView) findViewById(R.id.textLeftView);

        text = getResources().getString(R.string.typeText);
        currentWord = text.substring(0, text.indexOf(' '));
        setCpm(MIN_CPM);

        cpmBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progress = i + MIN_CPM;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setCpm(progress);
            }
        });

        playFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRunning) {
                    startWriting();
                } else {
                    stopWriting();
                }
            }
        });
    }

    private void notifyWearable(char c) {
        if (deviceReady()) {
            bluetoothService.setVibration(getVibrationScheme(c));
        }
    }

    private boolean deviceReady() {
        return bound && cpm < maxAllowedCpm && connected && discovered && monitorCount != -1;
    }

    private byte[] noVibration() {
        if (monitorCount != -1) {
            return new byte[monitorCount];
        } else {
            Log.d(BLE_LOG, "Monitor count not set yet.");
            return new byte[5];
        }
    }

    private byte[] getVibrationScheme(char c) {
        // TODO: check monitor count
        return new byte[5];
    }

    private void stopWriting() {
        isRunning = false;
        handler.removeCallbacks(runnable);
        playFab.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_play));
        if (deviceReady()) {
            bluetoothService.setVibration(noVibration());
        }
    }

    private void startWriting() {
        isRunning = true;
        handler.removeCallbacks(runnable);
        handler.post(runnable);
        playFab.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stop));
    }

    private int getDelay() {
        double res = 60000.0 / cpm;
        return (int) res;
    }

    private void setCpm(int value) {
        this.cpm = value;
        cpmView.setText(String.valueOf(value));
    }
}
