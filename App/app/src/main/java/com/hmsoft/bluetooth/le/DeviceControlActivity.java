        package com.hmsoft.bluetooth.le;

        import com.example.bluetooth.le.R;

        import android.app.Activity;
        import android.bluetooth.BluetoothDevice;
        import android.content.BroadcastReceiver;
        import android.content.ComponentName;
        import android.content.Context;
        import android.content.Intent;
        import android.content.IntentFilter;
        import android.content.ServiceConnection;
        import android.os.Bundle;
        import android.os.IBinder;
        import android.util.Log;
        import android.view.Menu;
        import android.view.MenuItem;
        import android.view.View;
        import android.view.inputmethod.InputMethodManager;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.Toast;


/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private EditText edtSend;

    private Button btnSend;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            Log.e(TAG, "mBluetoothLeService is okay");
            // Automatically connects to the device upon successful start-up initialization.
            //mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {  //连接成功
                Log.e(TAG, "Only gatt, just wait");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) { //断开连接
                mConnected = false;
                invalidateOptionsMenu();
                btnSend.setEnabled(false);
            }else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) //可以开始干活了
            {
                mConnected = true;
                ShowDialog();
                btnSend.setEnabled(true);
                Log.e(TAG, "In what we need");
                invalidateOptionsMenu();
            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {                                        //初始化
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainlayout);

        final Intent intent = getIntent();
        mDeviceName = "LED";
        mDeviceAddress = "00:15:83:00:77:AF";
        System.out.println(mDeviceName + " " + mDeviceAddress);

        // Sets up UI references.
        edtSend = (EditText) this.findViewById(R.id.editText);

        btnSend = (Button) this.findViewById(R.id.button);
        btnSend.setOnClickListener(new ClickEvent());
        btnSend.setEnabled(false);


        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        Log.d(TAG, "Try to bindService=" + bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE));

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //this.unregisterReceiver(mGattUpdateReceiver);
        //unbindService(mServiceConnection);
        if(mBluetoothLeService != null)
        {
            mBluetoothLeService.close();
            mBluetoothLeService = null;
        }
        Log.d(TAG, "We are in destroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {                              //点击按钮
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                if(mConnected)
                {
                    mBluetoothLeService.disconnect();
                    mConnected = false;
                }
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void ShowDialog()
    {
        Toast.makeText(this, "连接成功，现在可以正常通信！", Toast.LENGTH_SHORT).show();
    }

    // 按钮事件
    class ClickEvent implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            System.out.println("onClick");
            String sendstr = null;
            switch (v.getId()){
                case R.id.button:
                    if(!mConnected) return;
                    if (edtSend.length() < 1) {
                        Toast.makeText(DeviceControlActivity.this, "请输入要发送的内容", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendstr = edtSend.getText().toString() + "\r\n";
                    mBluetoothLeService.WriteValue(sendstr);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if(imm.isActive())
                        imm.hideSoftInputFromWindow(edtSend.getWindowToken(), 0);
                    //todo Send data
                    break;
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {                        //注册接收的事件
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothDevice.ACTION_UUID);
        return intentFilter;
    }
}