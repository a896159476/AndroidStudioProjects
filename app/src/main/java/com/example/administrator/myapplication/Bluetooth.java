package com.example.administrator.myapplication;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class Bluetooth {

    private BluetoothDevice bleDevice;
    private int rssi;
    private byte[] scanRecord;


    public Bluetooth(BluetoothDevice bleDevice, int rssi, byte[] scanRecord) {
        this.bleDevice = bleDevice;
        this.rssi = rssi;
        this.scanRecord = scanRecord;
    }

    public BluetoothDevice getBleDevice() {
        return bleDevice;
    }

    public void setBleDevice(BluetoothDevice bleDevice) {
        this.bleDevice = bleDevice;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public byte[] getScanRecord() {
        return scanRecord;
    }

    public void setScanRecord(byte[] scanRecord) {
        this.scanRecord = scanRecord;
    }
}
