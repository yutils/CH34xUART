package com.hehongdan.ch34xuartdriver;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * 类描述：Ch34读取线程。
 *
 * @author hehongdan
 * @version v2019/11/26
 * @date 2019/11/26
 * @change yujing 2022年2月11日09:55:33
 */
public class Ch34ReadThread extends Thread {
    // 厂家芯片操作主类（增加的）
    private CH34xUARTDriver ch34xUARTDriver;
    //USB端点（D）
    private UsbEndpoint usbEndpoint;
    //USB接收控制器（E）
    //此类用于向USB设备发送和接收数据以及控制消息。 此类的实例由{@link UsbManager#openDevice}创建。
    private UsbDeviceConnection usbDeviceConnection;
    //字节缓冲区
    private ByteBuffer[] byteBuffers = new ByteBuffer[CH34xUARTDriver.REQUEST_COUNT];
    /**
     * 数据监听
     */
    private BytesListener listener;

    public Ch34ReadThread(CH34xUARTDriver driver, UsbEndpoint endpoint, UsbDeviceConnection deviceConnection) {
        this.ch34xUARTDriver = driver;
        this.usbEndpoint = endpoint;
        this.usbDeviceConnection = deviceConnection;
        for (int i = 0; i < CH34xUARTDriver.REQUEST_COUNT; ++i) {
            //初始化读写工具
            driver.getUsbRequests()[i] = new UsbRequest();
            driver.getUsbRequests()[i].initialize(this.usbDeviceConnection, this.usbEndpoint);
            //分配字节缓冲区（容量）。
            byteBuffers[i] = ByteBuffer.allocate(this.ch34xUARTDriver._32);
        }
        //更改线程优先级(1-10,10最优先)
        this.setPriority(10);
    }

    @Override
    public void run() {
        super.run();
        /** 不同的USB接口下标（一个设备多个USB） */
        for (int i = 0; i < byteBuffers.length; ++i) {
            /**
             * 读取数据，初始化32位到缓冲区
             *
             * UsbRequest#queue()：将请求排队以在其端点上发送或接收数据。
             *
             * <p>对于OUT端点，给定的缓冲区数据将在端点上发送。对于IN端点，端点将尝试将给定数量的字节读取到指定的缓冲区中。
             * 如果排队操作成功，则返回true。结果将通过{@link UsbDeviceConnection#requestWait}返回
             * </p>
             * @param buffer 该缓冲区包含要写入的字节或用于存储读取结果的位置。
             *               位置和数组偏移将被忽略，并假定为0。极限和容量将被忽略。
             *               一旦请求{@link UsbDeviceConnection#requestWait()得到处理}，位置将被设置为读/写的字节数。
             * @param length 要读取或写入的字节数。在{@value Build.VERSION_CODES#P}之前，
             *               大于16384字节的值将被截断为16384。在API {@value Build.VERSION_CODES#P}中及之后的任何长度值都是有效的。
             * @return true，如果排队操作成功
             * @deprecated 已过时， {@link UsbRequest#queue(ByteBuffer)} 代替。
             */
            this.ch34xUARTDriver.getUsbRequests()[i].queue(byteBuffers[i], this.ch34xUARTDriver._32);
        }
        int count = 0;
        root:
        while (!Thread.interrupted()) {
            do {
                //没有打开USB则返回
                if (!this.ch34xUARTDriver.isNullUsb()) break;
            } while (this.usbEndpoint == null && !Thread.interrupted());

            for (int i = 0; i < byteBuffers.length; ++i) {
                UsbRequest usbRequest = this.usbDeviceConnection.requestWait();
                if (!this.ch34xUARTDriver.isNullUsb() && count++ > byteBuffers.length) {
                    Log.v(CH34xUARTDriver.TAG, "USB已经关闭");
                    break root;
                }
                if (usbRequest == null) break;
                UsbRequest usbRequestI = this.ch34xUARTDriver.getUsbRequests()[i];
                if (usbRequest != usbRequestI) {
                    continue; //Log.d(CH34xUARTDriver.TAG, "第" + (i + 1) + "个没有匹配到端口。" + "用户的=" + usbRequestI + "，对比的=" + usbRequest);
                }
                count = 0;
                byte[] 缓冲区 = byteBuffers[i].array();
                int 缓冲区_长度 = byteBuffers[i].position();
                if (缓冲区_长度 > 0) {
                    try {
                        this.ch34xUARTDriver.getSemaphore().acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    byte[] bytes = Arrays.copyOf(缓冲区, 缓冲区_长度);
                    //Log.v("数据", Utils.bytesToHexString(bytes));
                    if (listener != null) listener.value(bytes);
                    this.ch34xUARTDriver.getSemaphore().release();
                }
                this.ch34xUARTDriver.getUsbRequests()[i].queue(byteBuffers[i], this.ch34xUARTDriver._32);
            }
        }

        for (ByteBuffer byteBuffer : byteBuffers) {
            byteBuffer.clear();
        }
        Log.v(CH34xUARTDriver.TAG, "退出读取线程");
    }

    /**
     * 设置数据监听
     *
     * @param listener
     */
    public void setListener(BytesListener listener) {
        this.listener = listener;
    }
}

