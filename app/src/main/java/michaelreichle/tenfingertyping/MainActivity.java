package michaelreichle.tenfingertyping;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import michaelreichle.tenfingertyping.DeviceListView.DeviceHolder;

import static michaelreichle.tenfingertyping.R.id.floatingActionButton;

public class MainActivity extends AppCompatActivity {

    public final static String RUNNABLE_LOG = "runnable";
    public final static String BLE_LOG = "ble";

    private final static int DEVICE_SCAN_REQUEST = 5;

    private FloatingActionButton playFab;
    private TextView cpmView;
    private TextView charView;
    private TextView leftView;
    private TextView rightView;
    private TextView textView;
    private TextView deviceStatusView;
    private TextView deviceNameView;

    private String text;
    private String currentWord = "";
    private int currentCharIndex = 0;

    private int cpm;
    private int MIN_CPM = 10;
    private int maxAllowedCpm = -1;

    private boolean isRunning = false;
    DeviceHolder deviceHolder = null;
    // in order not to call bindService twice
    private boolean onActivityResult = false;
    private ProgressDialog progressDialog;

    final Handler handler = new Handler();
    final Runnable runnable = new Runnable() {
        public void run() {
            if (text.isEmpty() || !isRunning){
                handler.removeCallbacks(this);
                Log.d(RUNNABLE_LOG,"stopped");
                if (text.isEmpty()) {
                    reset(getResources().getString(R.string.default_text_source));
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
                    String left = currentWord.substring(0, currentCharIndex);
                    String right = currentWord.substring(currentCharIndex + 1);
                    if (left.length() > 8) {
                        left = left.substring(left.length() - 8, left.length());
                    }
                    if (right.length() > 8) {
                        right = right.substring(0, 8);
                    }
                    leftView.setText(left);
                    charView.setText(String.valueOf(currentChar));
                    rightView.setText(right);
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
    private int monitorCount = 4;
    /** Defines callbacks for bluetoothService binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to BluetoothService, cast the IBinder and get LocalService instance
            BluetoothService.BluetoothBinder binder = (BluetoothService.BluetoothBinder) service;
            bluetoothService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            maxAllowedCpm = -1;
        }
    };

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getStringExtra(BluetoothService.ACTION_ARG);

            if (BluetoothService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                progressDialog.setMessage("Discover services...");
            } else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                progressDialog.cancel();
                connected = false;
                discovered = false;
                maxAllowedCpm = -1;
                Toast.makeText(MainActivity.this, "disconnected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                discovered = true;
                if (!bluetoothService.getMaxFrequency()) {
                    Toast.makeText(MainActivity.this, "Couldn't get frequency and monitor count.", Toast.LENGTH_SHORT).show();
                }
                progressDialog.setMessage("Get maximal frequency...");
            } else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (BluetoothService.RESULT_MONITOR_COUNT.equals(intent.getStringExtra(BluetoothService.RESULT_DATA))) {
                    monitorCount = intent.getIntExtra(BluetoothService.EXTRA_DATA, -1);
                } else if (BluetoothService.RESULT_FREQUENCY.equals(intent.getStringExtra(BluetoothService.RESULT_DATA))) {
                    int frequency = intent.getIntExtra(BluetoothService.EXTRA_DATA, -1);
                    maxAllowedCpm = frequency * 60;
                }
                progressDialog.cancel();
                if (deviceReady()) {
                    Toast.makeText(MainActivity.this, "Device ready!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Couldn't instantiate device", Toast.LENGTH_SHORT).show();
                }
            }
            setDeviceViews();
        }
    };

    private void setDeviceViews() {
        if (deviceReady()) {
            deviceStatusView.setText(R.string.ready);
            deviceNameView.setText(deviceHolder.getName());
        } else {
            if (deviceHolder != null) {
                deviceNameView.setText(deviceHolder.getName());
                deviceNameView.setText(R.string.pending);
            } else {
                deviceStatusView.setText("");
                deviceNameView.setText(R.string.none);
            }
        }
    }

    private void reset(String newText) {
        if (newText.equals("")) {
            Toast.makeText(this, "Text empty. Setting default text.", Toast.LENGTH_SHORT).show();
            newText = getResources().getString(R.string.default_text_source);
        }
        String l = getResources().getString(R.string.left);
        String r = getResources().getString(R.string.right);
        String c = getResources().getString(R.string.startingCharacter);
        leftView.setText(l);
        rightView.setText(r);
        charView.setText(c);
        textView.setText(newText);
        currentWord = "";
        this.text = newText;

        int i = this.text.indexOf(' ');
        if (i == -1) i = this.text.length();
        currentWord = this.text.substring(0, i);
        currentCharIndex = 0;
        stopWriting();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(gattUpdateReceiver, new IntentFilter(BluetoothService.BROADCAST_BLE_SERVICE));
        if (deviceHolder != null && deviceHolder.getDevice() != null) {
            if (!onActivityResult) {
                bindBluetoothService();
            }
        }
    }

    private void bindBluetoothService() {
        unbindBluetoothService();
        Intent intent = new Intent(this, BluetoothService.class);
        intent.putExtra(BluetoothService.DEVICE_ARG, deviceHolder.getDevice());
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        bound = true;
    }

    private void unbindBluetoothService() {
        if (bound) {
            unbindService(connection);
        }
        bound = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(gattUpdateReceiver);
        unbindBluetoothService();
        stopWriting();
        onActivityResult = false;
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
                showChooseTextVariantDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showConnectScreen() {
        DeviceScanActivity.start(this, DEVICE_SCAN_REQUEST, deviceHolder);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DEVICE_SCAN_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    deviceHolder = data.getParcelableExtra(DeviceScanActivity.DEVICE_EXTRA);
                    bindBluetoothService();
                    onActivityResult = true;
                    progressDialog.setTitle(deviceHolder.getName());
                    progressDialog.setMessage("Connecting...");
                    progressDialog.show();
                } else {
                    Toast.makeText(this, "Did not select a new BLE device.", Toast.LENGTH_SHORT).show();
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

        // start service so it doesn't get killed when changing activities.
        Intent intent = new Intent(this, BluetoothService.class);
        startService(intent);
    }



    private void init() {
        SeekBar cpmBar = (SeekBar) findViewById(R.id.seekBar);
        textView = (TextView) findViewById(R.id.text);
        playFab = (FloatingActionButton) findViewById(floatingActionButton);
        cpmView = (TextView) findViewById(R.id.cpm);
        charView = (TextView) findViewById(R.id.currentChar);
        rightView = (TextView) findViewById(R.id.textRightView);
        leftView = (TextView) findViewById(R.id.textLeftView);
        deviceNameView = (TextView) findViewById(R.id.device);
        deviceStatusView = (TextView) findViewById(R.id.device_status_text);

        text = getResources().getString(R.string.default_text_source);
        currentWord = text.substring(0, text.indexOf(' '));
        setCpm(MIN_CPM);
        setDeviceViews();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Connecting");
        progressDialog.setCancelable(true);
        progressDialog.setMessage("");

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
        if (deviceReady() && cpm < maxAllowedCpm ) {
            if (!bluetoothService.setVibration(getVibrationScheme(c))) {
                Log.d(BLE_LOG, "can't vibrate");
            } else {
                Log.d(BLE_LOG, "vibrating");
            }
        } else if (deviceReady() && bluetoothService.isVibrating()) {
            bluetoothService.setVibration(noVibration());
        }
    }

    private boolean deviceReady() {
        return deviceHolder != null && bound && connected && discovered && monitorCount != -1 && maxAllowedCpm != -1;
    }

    private byte[] noVibration() {
        if (monitorCount != -1) {
            return new byte[monitorCount];
        } else {
            Log.d(BLE_LOG, "Monitor count not set yet.");
            return new byte[4];
        }
    }

    private byte[] getVibrationScheme(char c) {
        c = Character.toLowerCase(c);
        int index = CharacterMap.getFingerIndex(c);
        if (index == CharacterMap.NO_FINGER || index >= monitorCount) {
            return noVibration();
        } else {
            byte[] res = new byte[monitorCount];
            res[index] = (byte) 0xFF;
            return res;
        }
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
        if (maxAllowedCpm != -1 && value > maxAllowedCpm) {
            Toast.makeText(this, "Chosen cpm is too high for wearable.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showChooseTextVariantDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Text Variant")
                .setItems(R.array.text_variants, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        setDefaultText();
                                        break;
                                    case 1:
                                        setRandomText();
                                        break;
                                    case 2:
                                        setInputText();
                                        break;
                                    case 3:
                                        filterOnlySupportedText();
                                        break;
                                    default:
                                        break;
                                }
                            }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setDefaultText() {
        reset(getResources().getString(R.string.default_text_source));
    }

    private void setRandomText() {
        reset(CharacterMap.getRandomSupportedText(monitorCount, 100));
    }

    private void setInputText() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final EditText input = new EditText(MainActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);

        builder.setTitle("Type Text")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        reset(input.getText().toString());
                        dialogInterface.cancel();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setView(input)
                .show();
    }

    private void filterOnlySupportedText() {
        reset(CharacterMap.filterSupported(this.text));
    }
}