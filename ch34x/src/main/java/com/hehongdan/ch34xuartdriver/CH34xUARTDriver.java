package com.hehongdan.ch34xuartdriver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;


/**
 * 类描述：CH34芯片主类。
 *
 * @author hehongdan
 * @version v2019/11/26
 * @date 2019/11/26
 * @change yujing 2022年2月11日09:55:33
 */
public class CH34xUARTDriver {
    public static final String TAG = "USB-CH34x";
    /**
     * HHD 是否显示土司
     */
    private boolean showToast = false;
    /**
     * HHD USB请求数据包个数
     */
    public static final int REQUEST_COUNT = 1;
    /**
     * HHD 每个USB请求数据包容量
     */
    public final int _32 = 32;

    /**
     * 系统上下文（mContext）
     */
    private Context context;
    /**
     * USB管理器（a）
     */
    private UsbManager usbManager;
    /**
     * 远程意图 （b），处理USB广播和USB权限。
     */
    public PendingIntent pendingIntent;
    /**
     * USB设备（c）  这个类表示一个连接到android设备的USB设备，android设备充当USB主机。每个设备都包含一个或多个{@link UsbInterface}，每个都包含若干{@link UsbEndpoint}（通过USB传输数据的通道）。<p>此类包含描述USB设备功能的信息（以及{@link UsbInterface}和{@link UsbEndpoint}）。要与设备通信，请为设备打开{@link UsbDeviceConnection}，并使用{@link UsbRequest}在终结点上发送和接收数据。{@link UsbDeviceConnection#controlTransfer}用于端点0上的控制请求。<div class=“special reference”><h3>开发人员指南</h3><p>有关与USB硬件通信的更多信息，请阅读USB</a></div>
     */
    private UsbDevice usbDevice;
    /**
     * USB接口（d）
     */
    private UsbInterface usbInterface;
    /**
     * USB终点（e）（表示{@link UsbInterface}上终结点的类。端点是通过USB发送和接收数据的通道。通常，大容量端点用于发送非平凡数量的数据。中断端点用于分别从主数据流发送少量数据（通常是事件）。端点0是从主机发送到设备的控制消息的特殊端点。当前不支持等时终结点。）
     */
    private UsbEndpoint usbEndpoint_e;
    /**
     * USB终点（f）
     */
    private UsbEndpoint usbEndpoint_f;
    /**
     * USB发送与接收控制器（g） 此类用于向USB设备发送和接收数据以及控制消息。 此类的实例由{@link UsbManager#openDevice}创建。
     */
    private UsbDeviceConnection usbDeviceConnection;
    /**
     * USB权限标识（广播）（h）
     */
    private String broadcastReceiverFilter;
    /**
     * 写（发送）数据时同步（锁）对象（j）
     */
    private final Object writeSynchronizedObject = new Object();
    /**
     * 注册广播或者已经连接（k）
     */
    private boolean isRegisterReceiver = false;
    /**
     * USB接口是否为空（l）
     */
    private boolean isNullUsb = false;
    /**
     * USB读取线程（m）
     */
    private Ch34ReadThread readThread;

    /**
     * 芯片支持的USB转串口线集合（t） 供应商ID：设备的产品ID，1a86:7523/1a86:5523/1a86:5512
     */
    private List<String> supportVendorProduct = new ArrayList<>();
    /**
     * 芯片支持的USB转串口种类个数（u） 供应商ID：设备的产品ID，1a86:7523/1a86:5523/1a86:5512
     */
    private int supportTypeSize;
    /**
     * 返回端点的最大数据包大小（v）。
     */
    private int maxPacketSize;
    /**
     * USB写（发送）数据的超时时间（增加的）（w）
     */
    private int writeTimeOut;
    /**
     * 初始化芯片默认超时（毫秒）（x）
     */
    private int timeOut_x = 500;
    /**
     * USB请求数据包的类（y）。这可用于在{@link UsbDeviceConnection}中读写数据。UsbRequests可用于在批量和中断端点上传输数据。批量端点上的请求可以通过{@link UsbDeviceConnection#bulkTransfer}同步发送，也可以通过{@link UsbRequest#queue}和{@link UsbDeviceConnection#requestWait}异步发送。中断端点上的请求仅异步发送和接收。<p>此类不支持端点0上的请求； 请将{@link UsbDeviceConnection#controlTransfer}用于终结点零请求。
     */
    private UsbRequest[] usbRequests = new UsbRequest[REQUEST_COUNT];

    /**
     * 同步（计数）信号量。（A） 从概念上讲，信号量维护一组许可证。 如有必要，每个{@link Semaphore#acquire}都会阻止，直到获得许可为止，然后获得许可。 每个{@link Semaphore#release}都会添加一个许可证，从而有可能释放阻止收购者。 但是，没有使用实际的许可对象。 {@code Semaphore}仅保留可用数量的计数并采取相应措施。
     */
    private Semaphore semaphore = new Semaphore(1);
    /**
     * USB设备状态变化广播接收器（B）
     */
    private final BroadcastReceiver ch34BroadcastReceiver = new Ch34BroadcastReceiver(this);

    /**
     * 数据监听
     */
    private BytesListener readListener;

    /**
     * 断开USB连接监听
     */
    private Listener closeListener;

    /**
     * 构造方法
     *
     * @param usbManager USB管理器
     * @param context    上下文
     * @param filter     广播过滤
     */
    public CH34xUARTDriver(UsbManager usbManager, Context context, String filter) {
        this.usbManager = usbManager;
        this.context = context;
        this.broadcastReceiverFilter = filter;
        //超时时间
        this.writeTimeOut = 10 * 1000;
        this.addSupportVendorProduct("1a86:7523");
        this.addSupportVendorProduct("1a86:5523");
        this.addSupportVendorProduct("1a86:5512");
    }

    /**
     * 枚举 CH34x 设备
     *
     * @return 返回枚举到的 CH34x 的设备，若无设备则返回 null
     * @see #resumeUsbList() 已经实现
     */
    public UsbDevice enumerateDevice() {
        this.usbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
        this.pendingIntent = PendingIntent.getBroadcast(this.context, 0, new Intent(this.broadcastReceiverFilter), PendingIntent.FLAG_IMMUTABLE);
        HashMap var1;
        if ((var1 = this.usbManager.getDeviceList()).isEmpty()) {
            if (showToast) {
                Toast.makeText(this.context, "没有设备或设备不匹配，No Device Or Device Not Match!", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "没有设备或设备不匹配，No Device Or Device Not Match!");
            }
        } else {
            for (Object o : var1.values()) {
                UsbDevice var2 = (UsbDevice) o;
                for (int i = 0; i < this.supportTypeSize; ++i) {
                    if (String.format("%04x:%04x", var2.getVendorId(), var2.getProductId()).equals(this.supportVendorProduct.get(i))) {
                        IntentFilter var5;
                        (var5 = new IntentFilter(this.broadcastReceiverFilter)).addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
                        this.context.registerReceiver(this.ch34BroadcastReceiver, var5);
                        this.isRegisterReceiver = true;
                        return var2;
                    }
                }
            }

        }
        return null;
    }

    /**
     * 打开（CH34x）设备。
     *
     * @param usbDevice 需要打开的 CH34x 设备
     * @see #resumeUsbList() 已经实现
     */
    public void openDevice(UsbDevice usbDevice) {
        this.pendingIntent = PendingIntent.getBroadcast(this.context, 0, new Intent(this.broadcastReceiverFilter), PendingIntent.FLAG_IMMUTABLE);
        if (this.usbManager.hasPermission(usbDevice)) {
            this.connectionDevice(usbDevice);
        } else {
            synchronized (this.ch34BroadcastReceiver) {
                this.usbManager.requestPermission(usbDevice, this.pendingIntent);
            }
        }
    }

    /**
     * 枚举并打开 CH34x 设备（操作流程为：resumeUsbList（或者 EnumerateDevice 后 OpenDevice）， UartInit， SetConfig，以上流程执行完后即可进行串口收发）
     * <p>这个函数包含了
     *
     * @return 返回 0 则成功，否则失败（-1 = 没有设备或设备不匹配）
     * @see #enumerateDevice()
     * @see #openDevice(UsbDevice)
     * 操作。
     * </>
     */
    public int resumeUsbList() {
        this.usbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
        this.pendingIntent = PendingIntent.getBroadcast(this.context, 0, new Intent(this.broadcastReceiverFilter), PendingIntent.FLAG_IMMUTABLE);
        HashMap var1;
        if ((var1 = this.usbManager.getDeviceList()).isEmpty()) {
            if (showToast) {
                Toast.makeText(this.context, "没有设备或设备不匹配，No Device Or Device Not Match", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "没有设备或设备不匹配，No Device Or Device Not Match");
            }
        } else {
            for (Object o : var1.values()) {
                UsbDevice var2 = (UsbDevice) o;
                for (int i = 0; i < this.supportTypeSize; ++i) {
                    if (String.format("%04x:%04x", var2.getVendorId(), var2.getProductId()).equals(this.supportVendorProduct.get(i))) {
                        Log.d(TAG, "文本格式匹配-->" + String.format("%04x:%04x", var2.getVendorId(), var2.getProductId()));
                        IntentFilter var6;
                        //广播 USB设备移除
                        (var6 = new IntentFilter(this.broadcastReceiverFilter)).addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
                        this.context.registerReceiver(this.ch34BroadcastReceiver, var6);
                        this.isRegisterReceiver = true;
                        if (this.usbManager.hasPermission(var2)) {
                            this.connectionDevice(var2);
                            return 0;
                        }
                        if (showToast) {
                            Toast.makeText(this.context, "没有权限，No Permission!", Toast.LENGTH_LONG).show();
                        } else {
                            Log.d(TAG, "没有权限，No Permission!");
                        }
                        synchronized (this.ch34BroadcastReceiver) {
                            this.usbManager.requestPermission(var2, this.pendingIntent);
                            return -2;
                        }
                    } else {
                        Log.d(TAG, "文本格式不匹配-->" + String.format("%04x:%04x", var2.getVendorId(), var2.getProductId()));
                    }
                }
            }
        }
        return -1;
    }

    /**
     * 设置初始化CH34x芯片（串口）  。
     *
     * @return 若初始化失败，则返回 false，成功返回 true
     */
    public boolean uartInit() {
        byte[] var2 = new byte[8];
        this.controlTransfer(161, 0, 0);
        if (this.controlTransfer(95, 0, 0, var2, 2) < 0) {
            return false;
        } else {
            this.controlTransfer(154, 4882, 55682);
            this.controlTransfer(154, 3884, 4);
            if (this.controlTransfer(149, 9496, 0, var2, 2) < 0) {
                return false;
            } else {
                this.controlTransfer(154, 10023, 0);
                this.controlTransfer(164, 255, 0);
                return true;
            }
        }
    }

    /**
     * 设置 UART 接口的波特率、数据位、停止位、奇偶校验位以及流控 。
     *
     * @param baudRate    波特率（波特率：300，600，1200、2400、4800、9600、19200、38400、57600、115200、 230400、460800、921600，默认：9600 ）
     * @param dataBits    数据位（：5 个数据位、6 个数据位、7 个数据位、8 个数据位，默认：8 个数据位）
     * @param stopBits    停止位（1 个停止位，1：2 个停止位，默认：1 个停止位 ）
     * @param parity      校验位（0：none，1：add，2：even，3：mark 和 4：space，默认：none ）
     * @param flowControl 串口流控制（none，1：cts/rts，默认：none ）
     * @return 若设置失败，则返回 false，成功返回 true
     */
    public boolean setConfig(int baudRate, byte dataBits, byte stopBits, byte parity, byte flowControl) {
        char var10;
        switch (parity) {
            case 0:
                var10 = 0;
                break;
            case 1:
                var10 = '\b';
                break;
            case 2:
                var10 = 24;
                break;
            case 3:
                var10 = '(';
                break;
            case 4:
                var10 = '8';
                break;
            default:
                var10 = 0;
        }

        if (stopBits == 2) {
            var10 = (char) (var10 | 4);
        }

        switch (dataBits) {
            case 5:
                var10 = var10;
                break;
            case 6:
                var10 = (char) (var10 | 1);
                break;
            case 7:
                var10 = (char) (var10 | 2);
                break;
            case 8:
                var10 = (char) (var10 | 3);
                break;
            default:
                var10 = (char) (var10 | 3);
        }

        var10 = (char) (var10 | 192);
        int var7 = 156 | var10 << 8;
        byte var6;
        short var9;
        switch (baudRate) {
            case 50:
                var6 = 0;
                var9 = 22;
                break;
            case 75:
                var6 = 0;
                var9 = 100;
                break;
            case 110:
                var6 = 0;
                var9 = 150;
                break;
            case 135:
                var6 = 0;
                var9 = 169;
                break;
            case 150:
                var6 = 0;
                var9 = 178;
                break;
            case 300:
                var6 = 0;
                var9 = 217;
                break;
            case 600:
                var6 = 1;
                var9 = 100;
                break;
            case 1200:
                var6 = 1;
                var9 = 178;
                break;
            case 1800:
                var6 = 1;
                var9 = 204;
                break;
            case 2400:
                var6 = 1;
                var9 = 217;
                break;
            case 4800:
                var6 = 2;
                var9 = 100;
                break;
            case 9600:
                var6 = 2;
                var9 = 178;
                break;
            case 19200:
                var6 = 2;
                var9 = 217;
                break;
            case 38400:
                var6 = 3;
                var9 = 100;
                break;
            case 57600:
                var6 = 3;
                var9 = 152;
                break;
            case 115200:
                var6 = 3;
                var9 = 204;
                break;
            case 230400:
                var6 = 3;
                var9 = 230;
                break;
            case 460800:
                var6 = 3;
                var9 = 243;
                break;
            case 500000:
                var6 = 3;
                var9 = 244;
                break;
            case 921600:
                var6 = 7;
                var9 = 243;
                break;
            case 1000000:
                var6 = 3;
                var9 = 250;
                break;
            case 2000000:
                var6 = 3;
                var9 = 253;
                break;
            case 3000000:
                var6 = 3;
                var9 = 254;
                break;
            default:
                var6 = 2;
                var9 = 178;
        }
        baudRate = 136 | var6 | var9 << 8;
        baudRate = this.controlTransfer(161, var7, baudRate);
        if (flowControl == 1) {
            byte var11 = 96;
            this.controlTransfer(164, ~var11, 0);
        }
        return baudRate >= 0;
    }

    /**
     * 发送（写出）数据
     *
     * @param data       数据（发送缓冲区）
     * @param dataLength 数据长度（发送的字节数）
     * @return 返回值为写成功的字节数。是否成功（返回传输数据的长度（或0）表示成功，或返回负值表示失败）
     * @throws Throwable 异常
     */
    public int writeData(byte[] data, int dataLength) /*throws Throwable*/ {
        int result = -1;
        try {
            result = this.writeData(data, dataLength, this.writeTimeOut);
        } catch (Throwable t) {
            t.printStackTrace();
            Log.d(TAG, "写出（发送）数据出错=" + t.getMessage());
        }
        return result;
    }

    /**
     * 发送（写出）数据
     *
     * @param data       数据（发送缓冲区）
     * @param dataLength 数据长度（发送的字节数）
     * @param timeOut    USB写（发送）数据的超时时间
     * @return 是否成功（返回传输数据的长度（或0）表示成功，或返回负值表示失败）
     * @throws Throwable 异常
     */
    public int writeData(byte[] data, int dataLength, int timeOut) throws Throwable {
        synchronized (this.writeSynchronizedObject) {
            int var5 = 0;
            int var7 = dataLength;
            if (this.usbEndpoint_f == null) return -1;
            while (var5 < dataLength) {
                int var15;
                byte[] var8 = new byte[var15 = Math.min(var7, this.maxPacketSize)];
                if (var5 == 0) {
                    System.arraycopy(data, 0, var8, 0, var15);
                } else {
                    System.arraycopy(data, var5, var8, 0, var15);
                }
                /**
                 * 在给定的终结点上执行批量事务。传输的方向由端点的方向决定。
                 *<p>此方法从缓冲区中的索引0开始传输数据。要指定不同的偏移量，请使用{@link UsbDeviceConnection#bulkTransfer(UsbEndpoint, byte[], int, int, int)}.。*</p>
                 *
                 *@param endpoint此事务的终结点
                 *@param buffer 用于发送或接收数据；可以是{@code null}，以等待下一个事务而不读取数据
                 *@param length 要发送或接收的数据的长度。在{@value Build.VERSION_CODES#P}之前，大于16384字节的值将被截断为16384。在API{@value Build.VERSION_CODES#P}和之后，任何长度值都是有效的。
                 *@param timeout（毫秒），0是无限的
                 *
                 *@返回 传输数据的长度（或0）表示成功，或返回负值表示失败
                 */
                if ((var15 = this.usbDeviceConnection.bulkTransfer(this.usbEndpoint_f, var8, var15, timeOut)) < 0) {
                    return -2;
                }
                var5 += var15;
                var7 -= var15;
            }
            return var5;
        }
    }

    /**
     * 关闭（串口）USB设备
     */
    public void closeDevice() {
        //连接监听
        if (closeListener != null) {
            Listener temp = closeListener;
            setCloseListener(null);
            temp.value();
        }

        setReadListener(null);

        //停止读取线程
        if (readThread != null) {
            readThread.interrupt();
            readThread = null;
        }

        //是否已经连接
        if (this.isNullUsb) {
            this.isNullUsb = false;
        }

        //释放USB接口；关闭USB设备连接器
        if (this.usbDeviceConnection != null) {
            if (this.usbInterface != null) {
                this.usbDeviceConnection.releaseInterface(this.usbInterface);
                this.usbInterface = null;
            }
            this.usbDeviceConnection.close();
        }

        //置空USB设备
        if (this.usbDevice != null) {
            this.usbDevice = null;
        }

        //置空USB管理器
        if (this.usbManager != null) {
            this.usbManager = null;
        }

        //注销广播接收器
        if (this.isRegisterReceiver) {
            this.context.unregisterReceiver(this.ch34BroadcastReceiver);
            this.isRegisterReceiver = false;
        }
    }

    /**
     * 判断USB是否已连接。判断设备是否已经连接到 Android 系统
     *
     * @return 是否连。返回为 false 时表示设备未连接到系统，true 表示设备已连接
     */
    public boolean isConnected() {
        return this.usbDevice != null && this.usbInterface != null && this.usbDeviceConnection != null;
    }

    /**
     * 设置超时。根据自己的设备来设置读写超时时间
     *
     * @param timeOut 设置写超时时间，默认为 10000ms。USB写（发送）数据的超时时间
     * @return 是否成功
     */
    public void setTimeOut(int timeOut) {
        this.writeTimeOut = timeOut;
    }

    /**
     * 添加支持的供应商ID和产品ID。（a）
     *
     * @param vendorAndProduct 供应商ID和产品ID
     */
    private void addSupportVendorProduct(String vendorAndProduct) {
        this.supportVendorProduct.add(vendorAndProduct);
        this.supportTypeSize = this.supportVendorProduct.size();
    }

    /**
     * （a）释放旧的；创建新的USB设备、USB接口、声明连接；启动接收（读）的线程。
     *
     * @param usbDevice USB设备。
     */
    private void connectionDevice(UsbDevice usbDevice) {
        if (usbDevice != null) {
            if (this.usbDeviceConnection != null) {
                if (this.usbInterface != null) {
                    //释放(USB接口)对{@link android.hardware.usb.UsbInterface}的独占访问。//如果成功释放返回true
                    this.usbDeviceConnection.releaseInterface(this.usbInterface);
                    this.usbInterface = null;
                }
                this.usbDeviceConnection.close();
                this.usbDevice = null;
                this.usbInterface = null;
            }
            UsbInterface usbInterface;
            int USB接口数量 = 0;//用于具有多种配置的设备
            while (true) {
                if (USB接口数量 >= usbDevice.getInterfaceCount()) {
                    usbInterface = null;
                    break;
                }
                UsbInterface var5;
                if ((var5 = usbDevice.getInterface(USB接口数量)).getInterfaceClass() == 255 && var5.getInterfaceSubclass() == 1 && var5.getInterfaceProtocol() == 2) {
                    usbInterface = var5;
                    break;
                }
                ++USB接口数量;
            }
            UsbInterface usbInterface_ = usbInterface;
            UsbDeviceConnection usbDeviceConnection;
            //USB设备不为空并且USB接口不为空
            //打开的设备连接器与当前类打开的一致
            //声明专有访问权（在收发数据之前完成此操作。）（参数：需要声明的接口，true = 在必要时断开内核驱动程序；返回：声明成功返回true）
            if (usbInterface_ != null && (usbDeviceConnection = this.usbManager.openDevice(usbDevice)) != null && usbDeviceConnection.claimInterface(usbInterface_, true)) {
                this.usbDevice = usbDevice;
                this.usbDeviceConnection = usbDeviceConnection;
                this.usbInterface = usbInterface_;
                CH34xUARTDriver ch34xUARTDriver = this;
                for (USB接口数量 = 0; USB接口数量 < usbInterface_.getEndpointCount(); ++USB接口数量) {
                    UsbEndpoint endpoint;
                    if ((endpoint = usbInterface_.getEndpoint(USB接口数量)).getType() == 2 && endpoint.getMaxPacketSize() == _32) {
                        /**
                         * .getDirection()：返回端点的方向。
                         * 如果方向是主机到设备，则返回{@link UsbConstants#USB_DIR_OUT}，如果方向是设备到主机，则返回{@link UsbConstants#USB_DIR_IN}。
                         *
                         * @see UsbConstants#USB_DIR_IN
                         * @see UsbConstants#USB_DIR_OUT
                         *  @返回端点的方向
                         */
                        if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                            ch34xUARTDriver.usbEndpoint_e = endpoint;
                        } else {
                            ch34xUARTDriver.usbEndpoint_f = endpoint;
                        }
                        ch34xUARTDriver.maxPacketSize = endpoint.getMaxPacketSize();
                    } else {
                        endpoint.getType();
                    }
                }
                if (!this.isNullUsb) {
                    this.isNullUsb = true;
                    if (readThread != null) readThread.interrupt();
                    this.readThread = new Ch34ReadThread(this, this.usbEndpoint_e, this.usbDeviceConnection);
                    this.readThread.setListener(readListener);
                    this.readThread.start();
                }
            }
        }
    }

    /**
     * USB（模式）特征支持
     *
     * @return 是否支持
     */
    public boolean usbFeatureSupported() {
        return this.context.getPackageManager().hasSystemFeature("android.hardware.usb.host");
    }

    /**
     * 页面重新启动时检查权限
     *
     * @return 是否有权限（-1 = 没有）
     */
    public int resumeUsbPermission() {
        this.usbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
        this.pendingIntent = PendingIntent.getBroadcast(this.context, 0, new Intent(this.broadcastReceiverFilter), PendingIntent.FLAG_IMMUTABLE);
        HashMap var1;
        if ((var1 = this.usbManager.getDeviceList()).isEmpty()) {
            if (showToast) {
                Toast.makeText(this.context, "没有设备或设备不匹配，No Device Or Device Not Match!", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "没有设备或设备不匹配，No Device Or Device Not Match!");
            }
        } else {
            for (Object o : var1.values()) {
                UsbDevice var2 = (UsbDevice) o;
                for (int i = 0; i < this.supportTypeSize; ++i) {
                    //.getVendorId()返回设备的供应商ID。.getProductId()返回设备的产品ID。
                    if (String.format("%04x:%04x", var2.getVendorId(), var2.getProductId()).equals(this.supportVendorProduct.get(i))) {
                        if (!this.usbManager.hasPermission(var2)) {
                            synchronized (this.ch34BroadcastReceiver) {
                                this.usbManager.requestPermission(var2, this.pendingIntent);
                                return -2;
                            }
                        }
                        return 0;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * USB设备的0端口上执行控制事务（a）
     *
     * @param var1     此事务的请求请求ID
     * @param var2     此事务的value字段
     * @param baudRate 波特率
     * @return 返回传输数据的长度（或0）表示成功，或返回负值表示失败
     */
    private int controlTransfer(int var1, int var2, int baudRate) {
        return this.usbDeviceConnection.controlTransfer(64, var1, var2, baudRate, null, 0, this.timeOut_x);
    }

    /**
     * USB设备的0端口上执行控制事务（a）
     *
     * @param var1 此事务的请求请求ID
     * @param var2 此事务的value字段
     * @param var3 （没用上）此事务的索引字段
     * @param var4 缓冲区用于事务的数据部分，如果不需要发送或接收数据，则为空
     * @param var5 （没用上）
     * @return 返回传输数据的长度（或0）表示成功，或返回负值表示失败
     */
    private int controlTransfer(int var1, int var2, int var3, byte[] var4, int var5) {
        /**
         * .controlTransfer()：
         * 在此设备的端点零上执行控制事务。 传输方向由请求类型决定。
         * 如果requestType和 & {@link UsbConstants#USB_ENDPOINT_DIR_MASK}为{@link UsbConstants#USB_DIR_OUT},，
         * 则该传输为写操作；如果为{@link UsbConstants#USB_DIR_IN},，则该传输为读操作。
         * <p>
         * 此方法从缓冲区中的索引0开始传输数据。 要指定其他偏移量，请使用{@link UsbDeviceConnection#controlTransfer(int, int, int, int, byte[], int, int, int)}.。
         * </p>
         *
         * @param requestType 此交易的请求类型
         * @param request 此交易的请求ID
         * @param value 此交易的值字段
         * @param index 此交易的索引字段
         * @param buffer 事务的数据部分的缓冲区；如果不需要发送或接收任何数据，则为null
         * @param length 发送或接收的数据长度
         * @param timeout 以毫秒为单位
         * @return 成功传输的数据（或为零）或失败的负值
         */
        return this.usbDeviceConnection.controlTransfer(192, var1, var2, 0, var4, 2, this.timeOut_x);
    }

    /**
     * 获取USB发送与接收控制器。
     *
     * @return 控制器。
     */
    public UsbDeviceConnection getUsbDeviceConnection() {
        return usbDeviceConnection;
    }

    /**
     * 获取USB所有请求数据包。
     *
     * @return 所有请求数据包。
     */
    public UsbRequest[] getUsbRequests() {
        return usbRequests;
    }

    /**
     * 获取获取当前USB设备。
     *
     * @return USB设备。
     */
    protected UsbDevice getUsbDevice() {
        return this.usbDevice;
    }

    /**
     * 获取上下文。
     *
     * @return 上下文。
     */
    public Context getContext() {
        return context;
    }

    /**
     * 判断USB接口是否打开。
     *
     * @return 是否打开。
     */
    public boolean isNullUsb() {
        return isNullUsb;
    }

    /**
     * 获取芯片支持的USB转串口种类个数。
     *
     * @return 种类个数。
     */
    public int getSupportTypeSize() {
        return supportTypeSize;
    }

    /**
     * 获取芯片支持的USB转串口线集合线集合。
     *
     * @return 支持的串口线集合。
     */
    public List<String> getSupportVendorProduct() {
        return supportVendorProduct;
    }

    /**
     * 获取获取权限标识。
     *
     * @return 权限标识。
     */
    public String getBroadcastReceiverFilter() {
        return broadcastReceiverFilter == null ? "" : broadcastReceiverFilter;
    }

    /**
     * 获取同步（计数）信号量。
     *
     * @return 信号量。
     */
    public Semaphore getSemaphore() {
        return semaphore;
    }

    /**
     * 获取是否显示Toast（土司）。
     *
     * @return 是否显示。
     */
    public boolean isShowToast() {
        return showToast;
    }

    /**
     * 设置是否显示Toast（土司）。
     *
     * @param showToast 是否显示。
     */
    public void setShowToast(boolean showToast) {
        this.showToast = showToast;
    }

    /**
     * 读取数据监听
     *
     * @param readListener
     */
    public void setReadListener(BytesListener readListener) {
        if (readThread != null) readThread.setListener(readListener);
        this.readListener = readListener;
    }

    /**
     * USB断开监听
     *
     * @param closeListener
     */
    public void setCloseListener(Listener closeListener) {
        this.closeListener = closeListener;
    }
}
