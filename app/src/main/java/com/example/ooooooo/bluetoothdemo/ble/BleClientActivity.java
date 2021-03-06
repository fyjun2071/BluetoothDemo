package com.example.ooooooo.bluetoothdemo.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.ooooooo.bluetoothdemo.APP;
import com.example.ooooooo.bluetoothdemo.R;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * BLE客户端(主机/中心设备/Central)
 */
public class BleClientActivity extends Activity {
    private static final String TAG = BleClientActivity.class.getSimpleName();

    /**
     * uart服务
     */
    public final static UUID UARTSERVICE_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * 写
     */
    public final static UUID UART_RX_CHARACTERISTIC_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * 读
     */
    public final static UUID UART_TX_CHARACTERISTIC_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

    private EditText mWriteET;
    private TextView mTips;
    private BleDevAdapter mBleDevAdapter;
    private BluetoothGatt mBluetoothGatt;
    private boolean isConnected = false;

    private Map<String, List<String>> readableCharacteristicMap = new HashMap<>();

    // 与服务端连接的Callback
    public BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BluetoothDevice dev = gatt.getDevice();
            Log.i(TAG, String.format("onConnectionStateChange:%s,%s,%s,%s", dev.getName(), dev.getAddress(), status, newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                gatt.discoverServices(); //启动服务发现
            } else {
                isConnected = false;
                closeConn();
            }
            logTv(String.format(status == 0 ? (newState == 2 ? "与[%s]连接成功" : "与[%s]连接断开") : ("与[%s]连接出错,错误码:" + status), dev));
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, String.format("onServicesDiscovered:%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), status));
            if (status == BluetoothGatt.GATT_SUCCESS) { //BLE服务发现成功
                // 遍历获取BLE服务Services/Characteristics/Descriptors的全部UUID
                for (BluetoothGattService service : gatt.getServices()) {
                    String serviceUUID = service.getUuid().toString();
                    StringBuilder allUUIDs = new StringBuilder("UUIDs={\nS=" + serviceUUID);
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        allUUIDs.append(",\nC=")
                                .append(characteristic.getUuid())
                                .append(",").append(characteristic.getProperties());
                        if ((BluetoothGattCharacteristic.PROPERTY_READ & characteristic.getProperties()) > 0) {
                            allUUIDs.append(",read");
                            if (!readableCharacteristicMap.containsKey(serviceUUID)) {
                                readableCharacteristicMap.put(serviceUUID, new ArrayList<String>());
                            }

                            readableCharacteristicMap.get(serviceUUID).add(characteristic.getUuid().toString());
                        }
                        if ((BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE & characteristic.getProperties()) > 0) {
                            allUUIDs.append(",write_no_response");
                        }
                        if ((BluetoothGattCharacteristic.PROPERTY_WRITE & characteristic.getProperties()) > 0) {
                            allUUIDs.append(",write");
                        }
                        if ((BluetoothGattCharacteristic.PROPERTY_NOTIFY & characteristic.getProperties()) > 0) {
                            allUUIDs.append(",notfiy");
                        }
                        if ((BluetoothGattCharacteristic.PROPERTY_INDICATE & characteristic.getProperties()) > 0) {
                            allUUIDs.append(",indicate");
                        }
                        if ((BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE & characteristic.getProperties()) > 0) {
                            allUUIDs.append(",signed_write");
                        }
                        if ((BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS & characteristic.getProperties()) > 0) {
                            allUUIDs.append(",extended_props");
                        }

                        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                            allUUIDs.append(",\nD=").append(descriptor.getUuid());
                            descriptor.getPermissions();
                        }
                    }
                    allUUIDs.append("\n}");
                    Log.i(TAG, "onServicesDiscovered:" + allUUIDs.toString());
                    logTv("发现服务" + allUUIDs);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue(), Charset.forName("GBK"));
            Log.i(TAG, String.format("onCharacteristicRead:%s,%s,%s,%s,%s,%s", gatt.getDevice().getName(),
                    gatt.getDevice().getAddress(), uuid, valueStr, status, Arrays.toString(characteristic.getValue())));
            logTv("读取Characteristic[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Log.i(TAG, String.format("onCharacteristicWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            logTv("写入Characteristic[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Log.i(TAG, String.format("onCharacteristicChanged:%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr));
            logTv("通知Characteristic[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getUuid();
            String valueStr = Arrays.toString(descriptor.getValue());
            Log.i(TAG, String.format("onDescriptorRead:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            logTv("读取Descriptor[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getUuid();
            String valueStr = Arrays.toString(descriptor.getValue());
            Log.i(TAG, String.format("onDescriptorWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            logTv("写入Descriptor[" + uuid + "]:\n" + valueStr);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleclient);
        RecyclerView rv = findViewById(R.id.rv_ble);
        mWriteET = findViewById(R.id.et_write);
        mTips = findViewById(R.id.tv_tips);
        rv.setLayoutManager(new LinearLayoutManager(this));
        mBleDevAdapter = new BleDevAdapter(new BleDevAdapter.Listener() {
            @Override
            public void onItemClick(BluetoothDevice dev) {
                closeConn();
                mBluetoothGatt = dev.connectGatt(BleClientActivity.this, false, mBluetoothGattCallback); // 连接蓝牙设备
                logTv(String.format("与[%s]开始连接............", dev));
            }
        });
        rv.setAdapter(mBleDevAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeConn();
    }

    // BLE中心设备连接外围设备的数量有限(大概2~7个)，在建立新连接之前必须释放旧连接资源，否则容易出现连接错误133
    private void closeConn() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }
    }

    // 扫描BLE
    public void reScan(View view) {
        if (mBleDevAdapter.isScanning) {
            APP.toast("正在扫描...", 0);
        } else {
            mBleDevAdapter.reScan();
        }
    }

    // 注意：连续频繁读写数据容易失败，读写操作间隔最好200ms以上，或等待上次回调完成后再进行下次读写操作！
    // 读取数据成功会回调->onCharacteristicChanged()
    public void read(View view) {
        BluetoothGattService service = getGattService(UARTSERVICE_SERVICE_UUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UART_TX_CHARACTERISTIC_UUID);//通过UUID获取可读的Characteristic
            mBluetoothGatt.readCharacteristic(characteristic);
        }

        for (Map.Entry<String, List<String>> entry : readableCharacteristicMap.entrySet()) {
            Log.e(TAG, "read service:" + entry.getKey());
            BluetoothGattService s = getGattService(UUID.fromString(entry.getKey()));
            if (s == null) {
                continue;
            }
            for (String item : entry.getValue()) {
                BluetoothGattCharacteristic c = s.getCharacteristic(UUID.fromString(item));
                mBluetoothGatt.readCharacteristic(c);
            }
        }
    }

    // 注意：连续频繁读写数据容易失败，读写操作间隔最好200ms以上，或等待上次回调完成后再进行下次读写操作！
    // 写入数据成功会回调->onCharacteristicWrite()
    public void write(View view) {
        BluetoothGattService service = getGattService(UARTSERVICE_SERVICE_UUID);
        if (service != null) {
            String text = mWriteET.getText().toString();
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UART_RX_CHARACTERISTIC_UUID);//通过UUID获取可写的Characteristic
            characteristic.setValue(text.getBytes()); //单次最多20个字节
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    // 设置通知Characteristic变化会回调->onCharacteristicChanged()
    public void setNotify(View view) {
        BluetoothGattService service = getGattService(UARTSERVICE_SERVICE_UUID);
        if (service != null) {
            // 设置Characteristic通知
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UART_TX_CHARACTERISTIC_UUID);//通过UUID获取可通知的Characteristic
            mBluetoothGatt.setCharacteristicNotification(characteristic, true);

            // 向Characteristic的Descriptor属性写入通知开关，使蓝牙设备主动向手机发送数据
//            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UART_TX_CHARACTERISTIC_UUID);
            // descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);//和通知类似,但服务端不主动发数据,只指示客户端读取数据
//            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//            mBluetoothGatt.writeDescriptor(descriptor);

            List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
            for (BluetoothGattDescriptor descriptor : descriptors) {
                if (descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                    mBluetoothGatt.writeDescriptor(descriptor);
                    Log.d(TAG, "startRead: " + "监听收数据");
                }
            }
        }
    }

    // 获取Gatt服务
    private BluetoothGattService getGattService(UUID uuid) {
        if (!isConnected) {
            APP.toast("没有连接", 0);
            return null;
        }
        BluetoothGattService service = mBluetoothGatt.getService(uuid);
        if (service == null)
            APP.toast("没有找到服务UUID=" + uuid, 0);
        return service;
    }

    // 输出日志
    private void logTv(final String msg) {
        if (isDestroyed())
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                APP.toast(msg, 0);
                mTips.append(msg + "\n\n");
            }
        });
    }
}