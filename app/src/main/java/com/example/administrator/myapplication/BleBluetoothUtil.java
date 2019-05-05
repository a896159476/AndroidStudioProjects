package com.example.administrator.myapplication;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.reactivex.ObservableEmitter;

public class BleBluetoothUtil {

    private List<BluetoothDevice> bluetoothDevice = new ArrayList<>();

    public List<BluetoothDevice> getBluetoothDevice(){
        return bluetoothDevice;
    }

    private static volatile BleBluetoothUtil instance;
    private BleBluetoothUtil() {}
    public static BleBluetoothUtil getInstance() {
        if (instance == null) {
            synchronized (BleBluetoothUtil.class) {
                if (instance == null) {
                    instance = new BleBluetoothUtil();
                }
            }
        }
        return instance;
    }

    private BluetoothAdapter mBluetoothAdapter;
    private Context context;
    //是否支持蓝牙
    private boolean isBluetooth = true;
    private boolean isBluetoothBle = true;

    //只需初始化一次，必须在Application中调用。
    public void init(Application application){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null){
            isBluetooth = false;
        }
        PackageManager pm = application.getPackageManager();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (!pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
                isBluetoothBle = false;
            }
        }

        context = application;
    }

    /**
     * 开启蓝牙
     * @param activity
     */
    public void startBluetooth(Activity activity){
        if (!isBluetooth){
            showToast("该设备不支持蓝牙！");
            return;
        }
        if (!isBluetoothBle){
            showToast("该设备不支持BLE蓝牙！");
            return;
        }

        //未打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivity(enableBtIntent);
        }else {
            //检测gps是否打开（定位权限已经申请，如果没有申请需要检测）
            if (!isOpen(activity)){
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                activity.startActivity(intent);
            }
        }
    }

    private ObservableEmitter<List<Bluetooth>> emitter;
    /**
     * 搜索蓝牙
     */
    public void searchBluetooth(Activity activity, final ObservableEmitter<List<Bluetooth>> emitter){
        this.emitter = emitter;
        if (!isBluetooth){
            showToast("该设备不支持蓝牙！");
            return;
        }
        if (!isBluetoothBle){
            showToast("该设备不支持BLE蓝牙！");
            return;
        }
        //未打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            startBluetooth(activity);
            return;
        }
        //检测gps是否打开（定位权限已经申请，如果没有申请需要检测）
        if (!isOpen(activity)){
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.startActivity(intent);
        }else {
            Set<BluetoothDevice> pairedDevices = getBondedDevices();
            bluetoothDevice.clear();
            if (pairedDevices != null) {
                for (BluetoothDevice device: pairedDevices){
                    try {
                        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID.randomUUID());
                        bluetoothDevice.add(device);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            //搜索蓝牙
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
            }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
                mBluetoothAdapter.stopLeScan(leScanCallback);
            }
        }
    }

    //停止扫描
    public void stopScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            emitter.onNext(bluetooths);
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
            i = 0;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBluetoothAdapter.stopLeScan(leScanCallback);
            emitter.onNext(bluetooths);
            i = 0;
        }
    }

    private List<Bluetooth> bluetooths = new ArrayList<>();
    private int i = 0;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice bleDevice = result.getDevice();
            Bluetooth bluetooth = new Bluetooth(bleDevice,result.getRssi(),result.getScanRecord().getBytes());
            if (i == 0){
                bluetooths.clear();
                bluetooths.add(bluetooth);
                i++;
                return;
            }
            for (Bluetooth isBluetooths :bluetooths){
                String address = isBluetooths.getBleDevice().getAddress();
                if (address.equals(bleDevice.getAddress())){
                    return;
                }
            }
            i++;
            bluetooths.add(bluetooth);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Bluetooth bluetooth = new Bluetooth(device,rssi,scanRecord);
            if (i == 0){
                bluetooths.clear();
                bluetooths.add(bluetooth);
                i++;
                return;
            }
            for (Bluetooth isBluetooths :bluetooths){
                String address = isBluetooths.getBleDevice().getAddress();
                if (address.equals(device.getAddress())){
                    return;
                }
            }
            i++;
            bluetooths.add(bluetooth);
        }
    };

    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic alertLevel;

    public void connect(List<Bluetooth> bluetoothList,int position){
        BluetoothDevice device = bluetoothList.get(position).getBleDevice();
        //第二个参数表示是否需要自动连接。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = device.connectGatt(context, false,bluetoothGattCallback, 2);
        }
    }

    public void connectBluetoothDevice(List<BluetoothDevice> bluetoothList,int position){
        BluetoothDevice device = bluetoothList.get(position);

        //第二个参数表示是否需要自动连接。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBluetoothGatt = device.connectGatt(context, false,bluetoothGattCallback);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        //连接和断开连接的回调
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            super.onConnectionStateChange(gatt, status, newState);
            /*
             * 连接状态：
             *    * 断开连接   *public static final int STATE_DISCONNECTED  = 0;
             *    * 连接中     *public static final int STATE_CONNECTING    = 1;
             *    * 连接成功   *public static final int STATE_CONNECTED     = 2;
             *    * 断开中     *public static final int STATE_DISCONNECTING = 3;
             *
             */
            if (BluetoothGatt.STATE_CONNECTED == newState) {
                //连接成功
                gatt.discoverServices();//必须有，可以让onServicesDiscovered显示所有Services
            }else if (BluetoothGatt.STATE_DISCONNECTED == newState){
                //断开连接
                showToast("断开连接");
            }
        }
        //发现服务的回调
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> list = gatt.getServices();
            for (BluetoothGattService bluetoothGattService:list){
                //String str = bluetoothGattService.getUuid().toString();
                List<BluetoothGattCharacteristic> gattCharacteristics = bluetoothGattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    //gattCharacteristic.getUuid().toString()获取设备uuid
//                    if("00002a00-0000-1000-8000-00805f9b34fb".equals(gattCharacteristic.getUuid().toString())){
//                        BluetoothGattService linkLossService=bluetoothGattService;
//                        alertLevel=gattCharacteristic;
//                    }
                    alertLevel=gattCharacteristic;
                    dataSend();
                }

            }
            enableNotification(true,gatt,alertLevel);//必须要有，否则接收不到数据

        }

        //成功读取数据的回调
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            showToast( characteristic.getValue().toString()+"");
        }

        /**
         *  发送数据后的回调
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            super.onCharacteristicWrite(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS){//写入成功
                showToast("写入成功");
            }else if (status == BluetoothGatt.GATT_FAILURE){
                showToast("写入失败");
            }else if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED){
                showToast("没权限");
            }
        }

        //Characteristic 改变，数据接收会调用
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            showToast(characteristic.getValue()+"");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void enableNotification(boolean enable, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (gatt == null || characteristic == null)
            return; //这一步必须要有 否则收不到通知
        gatt.setCharacteristicNotification(characteristic, enable);
    }

    /**
     * 向蓝牙发送数据
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void dataSend(){
        //byte[] send={(byte) 0xaa,0x01,0x01,(byte)0x81,(byte) 0xff};
//        byte[] send = new byte[20];
//        send = hexStringToBytes("123");

        byte[] send={(byte) 0xaa,0x01,0x01,(byte)0x81,(byte) 0xff};
        byte[] sendData=new byte[send.length+2];
        sendData[0]=(byte) 0xaa;
        sendData[sendData.length-1]=(byte) 0xff;
        for(int i=1;i<sendData.length-1;i++){
            sendData[i]=send[i-1];
        }
//        Log.e("dataSend", bytesToHexString(sendData));
//        Log.e("dataSend", linkLossService +"");
        alertLevel.setValue(sendData);
        boolean status = mBluetoothGatt.writeCharacteristic(alertLevel);
        Log.e("dataSend", status+"");
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void disconnect(){
        mBluetoothGatt.disconnect();
    }

    //查询已配对的设备集
    private Set<BluetoothDevice> getBondedDevices(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() >0){
            return pairedDevices;
        }
        return null;
    }

    /**
     * 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
     * @param context
     * @return true 表示开启
     */
    private boolean isOpen(Context context) {
        LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }
        return false;
    }

    private void showToast(String msg){
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }
}
