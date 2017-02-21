package project.com.panzq.mybluetooth2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 1，首先开启蓝牙
 * 2，搜索可用设备
 * 3，创建蓝牙socket，获取输入输出流
 * 4，读取和写入数据
 * 5，断开连接关闭蓝牙
 */
public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tv_mac;
    private Button btn_on, btn_off, btn_scan;
    private ListView bluetooth_list;
    private static final String TAG = "panzq";
    private BluetoothAdapter mBluetoothAdapter = null;
    private List<String> deviceList = new ArrayList<String>();
    private ArrayAdapter<String> deviceAdapter;
    private DeviceReceiver myDevice = new DeviceReceiver();
    private static final int SHOW_MACINFO = 0;
    private boolean hasRegister = false;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SHOW_MACINFO:
                    if (mBluetoothAdapter.isEnabled()) {
                        String bluetoothName = mBluetoothAdapter.getName();
                        String macAddress = mBluetoothAdapter.getAddress();
                        Log.d(TAG, "macAddress : " + macAddress);
                        tv_mac.setText(bluetoothName + " -- " + macAddress);
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "蓝牙设备不可用", Toast.LENGTH_SHORT).show();
            return;
        }
        findViews();
        deviceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceList);
        bluetooth_list.setAdapter(deviceAdapter);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!hasRegister)
        {
            hasRegister = true;
            IntentFilter filterStart = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            IntentFilter filterFinish = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(myDevice,filterStart);
            registerReceiver(myDevice,filterFinish);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering())
        {
            mBluetoothAdapter.cancelDiscovery();
        }
        if (hasRegister)
        {
            hasRegister = false;
            unregisterReceiver(myDevice);
        }
    }

    private void findViews() {
        tv_mac = (TextView) findViewById(R.id.tv_mac);
        bluetooth_list = (ListView) findViewById(R.id.bluetooth_list);
        btn_on = (Button) findViewById(R.id.bluetooth_on);
        btn_off = (Button) findViewById(R.id.bluetooth_off);
        btn_scan = (Button) findViewById(R.id.bluetooth_scan);
        btn_on.setOnClickListener(this);
        btn_off.setOnClickListener(this);
        btn_scan.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bluetooth_on:
                Log.d(TAG, "turn on bluetooth ... ");
                if (mBluetoothAdapter != null) {
                    //确认蓝牙是否打开
                    if (!mBluetoothAdapter.isEnabled()) {
                        //请求用户开启
                        Intent intent_on = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent_on, RESULT_FIRST_USER);
                        //使蓝牙设备可见，方便配对
                        Intent intent_visible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        intent_visible.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 200);
                        startActivity(intent_visible);
                        //直接开启，不经过提示
                        //mBluetoothAdapter.enable();
                    }


                }

                break;
            case R.id.bluetooth_off:
                Log.d(TAG, "turn off bluetooth ... ");
                if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.disable();
                }

                break;
            case R.id.bluetooth_scan:
                Log.d(TAG, "scan bluetooth devices... ");
                if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {

                    deviceList.clear();
                    deviceAdapter.notifyDataSetChanged();
                    //如果正在扫描则关闭扫描
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                        btn_scan.setText("repet search");
                    } else {
                        //扫描已经配对的蓝牙设备
                        searchAvalibleDevice();
                        mBluetoothAdapter.startDiscovery();
                        btn_scan.setText("stop search");
                    }
                }

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            //在开启蓝牙时，对话框点击确认键时会走到这里
            case RESULT_OK:
                Log.d(TAG, "RESULT_OK");
                SendMessageDelay(SHOW_MACINFO, 500);
                break;
            //在开启蓝牙时，对话框点击取消键时会走到这里
            case RESULT_CANCELED:
                Log.d(TAG, "RESULT_CANCELED");
                break;
        }
    }

    private void SendMessageDelay(int what, long duration) {
        if (mHandler.hasMessages(what)) {
            mHandler.removeMessages(what);
        }
        mHandler.sendEmptyMessageDelayed(what, duration);
    }

    //扫描已经配对过的蓝牙设备
    private void searchAvalibleDevice() {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
            deviceList.clear();
            deviceAdapter.notifyDataSetChanged();
        }
        if (devices.size() > 0) {
            for (Iterator<BluetoothDevice> it = devices.iterator(); it.hasNext(); ) {
                BluetoothDevice device = it.next();
                deviceList.add(device.getName() + "\r\n" + device.getAddress());
                deviceAdapter.notifyDataSetChanged();
            }

        } else {
            deviceList.add("没有已配对的蓝牙设备");
            deviceAdapter.notifyDataSetChanged();
        }
    }

    private class DeviceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,"action = "+action);
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getBondState() != BluetoothDevice.BOND_BONDED)
                {
                    deviceList.add(device.getName()+"\r\n"+device.getAddress());
                    deviceAdapter.notifyDataSetChanged();
                }

            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                if (bluetooth_list.getCount() == 0)
                {
                    deviceList.add("没有可配对蓝牙设备");
                    deviceAdapter.notifyDataSetChanged();
                }
                btn_scan.setText("repeat srarch");
            }

        }
    }
}
