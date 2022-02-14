package com.hehongdan.ch34xuartdriver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.widget.Toast;

/**
 * 类描述：CH34广播接收器。
 *
 * @author hehongdan
 * @version v2019/11/26
 * @date 2019/11/26
 * @change yujing 2022年2月11日09:55:33
 */
public class Ch34BroadcastReceiver extends BroadcastReceiver {
    private CH34xUARTDriver ch34xUARTDriver;
    public Ch34BroadcastReceiver(CH34xUARTDriver driver) {
        super();
        this.ch34xUARTDriver = driver;
    }
    @Override
    public final void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        //广播 USB启动
        if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(action)) {
            Log.d(CH34xUARTDriver.TAG, "Step1! USB启动");
            //广播 USB权限
            //} else if (CH34xUARTDriver.a(this.ch34xUARTDriver).equals(var5)) {
        } else if (this.ch34xUARTDriver.getBroadcastReceiverFilter().equals(action)) {
            Log.d(CH34xUARTDriver.TAG, "Step2! 判断权限");
            synchronized (CH34xUARTDriver.class) {
                UsbDevice usbDevice = intent.getParcelableExtra("device");
                if (intent.getBooleanExtra("permission", false)) {
                    //有USB权限
                    //CH34xUARTDriver.a(this.ch34xUARTDriver, var9);
                    this.ch34xUARTDriver.openDevice(usbDevice);
                } else {
                    //广播 拒绝USB权限
                    if (this.ch34xUARTDriver.isShowToast()) {
                        Toast.makeText(this.ch34xUARTDriver.getContext(), "拒绝USB权限，Deny USB Permission!", Toast.LENGTH_LONG).show();
                    }
                    Log.d(CH34xUARTDriver.TAG, "拒绝USB权限，Deny USB Permission!");
                }
            }
            //广播 USB设备移除
        } else if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(action)) {
            //USB设备
            UsbDevice var6;
            //USB设备名称
            String var3 = (var6 = intent.getParcelableExtra("device")).getDeviceName();
            Log.d(CH34xUARTDriver.TAG, "Step3! USB设备被移除，USB Disconnect! 设备名称=" + var3);
            for (int i = 0; i < this.ch34xUARTDriver.getSupportTypeSize(); ++i) {
                if (String.format("%04x:%04x", var6.getVendorId(), var6.getProductId()).equals(this.ch34xUARTDriver.getSupportVendorProduct().get(i))) {
                    if (this.ch34xUARTDriver.isShowToast()) {
                      Toast.makeText(this.ch34xUARTDriver.getContext(), "USB设备被移除", Toast.LENGTH_LONG).show();
                    }
                    this.ch34xUARTDriver.closeDevice();
                }
            }
        } else {
            Log.e(CH34xUARTDriver.TAG, "没匹配到USB设备广播=" + action);
        }
    }
}