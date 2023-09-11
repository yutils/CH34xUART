package com.hehongdan.ch34xuartdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import com.hehongdan.ch34xuartdemo.databinding.MainBinding;
import com.hehongdan.ch34xuartdriver.CH34xUARTDriver;
import com.hehongdan.ch34xuartdriver.Utils;

import java.util.Arrays;

/**
 * 首页
 *
 * @author yujing 2022年2月14日11:15:42
 */
public class MainActivity extends Activity {
    /* DataBinding */
    private MainBinding binding;
    /* 当前是否打开 */
    private boolean isOpen;
    /* 波特率 */
    public int baudRate = 9600;
    /* 停止位 */
    public byte stopBit = 1;
    /* 数据位 */
    public byte dataBit = 8;
    /* 校验位 */
    public byte parity = 0;
    /* 串口流控制 */
    public byte flowControl = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.main);
        MyApp.driver = new CH34xUARTDriver((UsbManager) getSystemService(Context.USB_SERVICE), this, "cn.wch.wchusbdriver.USB_PERMISSION");
        MyApp.driver.setShowToast(true);
        initUI();
        //保持常亮的屏幕的状态
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //默认为断开
        close();

        //连接断开监听
        MyApp.driver.setCloseListener(() -> {
            Toast.makeText(this, "断开USB设备", Toast.LENGTH_LONG).show();
            close();
        });

        //打开按钮
        binding.openButton.setOnClickListener(v -> {
            //如果是打开，就关闭
            if (isOpen) {
                close();
                return;
            }
            open();
        });

        //配置按钮
        binding.configButton.setOnClickListener(v -> {
            //配置串口波特率，函数说明可参照编程手册
            if (MyApp.driver.setConfig(baudRate, dataBit, stopBit, parity, flowControl)) {
                Toast.makeText(MainActivity.this, "配置成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "配置失败!", Toast.LENGTH_SHORT).show();
            }
        });

        //写入按钮
        binding.writeButton.setOnClickListener(v -> {
            byte[] to_send = Utils.hexStringToByte(binding.writeText.getText().toString());//以16进制发送
            Toast.makeText(MainActivity.this, "发送：" + Arrays.toString(to_send), Toast.LENGTH_SHORT).show();
            //byte[] to_send = toByteArray2(writeText.getText().toString());//以字符串方式发送
            int result = MyApp.driver.writeData(to_send, to_send.length);//写数据，第一个参数为需要发送的字节数组，第二个参数为需要发送的字节长度，返回实际发送的字节长度
            if (result < 0) {
                Toast.makeText(MainActivity.this, "写入失败!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //处理界面
    private void initUI() {
        /* 波特率 */
        ArrayAdapter<CharSequence> baudAdapter = ArrayAdapter.createFromResource(this, R.array.baud_rate, R.layout.my_spinner_textview);
        baudAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        binding.baudSpinner.setAdapter(baudAdapter);
        binding.baudSpinner.setGravity(Gravity.CENTER_HORIZONTAL);
        binding.baudSpinner.setSelection(5);//默认选择第几个

        /* 停止位 */
        ArrayAdapter<CharSequence> stopAdapter = ArrayAdapter.createFromResource(this, R.array.stop_bits, R.layout.my_spinner_textview);
        stopAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        binding.stopSpinner.setAdapter(stopAdapter);
        binding.stopSpinner.setGravity(Gravity.CENTER_HORIZONTAL);

        /* 数据位 */
        ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(this, R.array.data_bits, R.layout.my_spinner_textview);
        dataAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        binding.dataSpinner.setAdapter(dataAdapter);
        binding.dataSpinner.setGravity(Gravity.CENTER_HORIZONTAL);
        binding.dataSpinner.setSelection(3);//默认选择第几个

        /* 校验位 */
        ArrayAdapter<CharSequence> parityAdapter = ArrayAdapter.createFromResource(this, R.array.parity, R.layout.my_spinner_textview);
        parityAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        binding.paritySpinner.setAdapter(parityAdapter);
        binding.paritySpinner.setGravity(Gravity.CENTER_HORIZONTAL);

        /* 串口流控制 */
        ArrayAdapter<CharSequence> flowAdapter = ArrayAdapter.createFromResource(this, R.array.flow_control, R.layout.my_spinner_textview);
        flowAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        binding.flowSpinner.setAdapter(flowAdapter);
        binding.flowSpinner.setGravity(Gravity.CENTER_HORIZONTAL);

        /* 波特率 */
        binding.baudSpinner.setOnItemSelectedListener(new MyOnBaudSelectedListener());
        /* 停止位 */
        binding.stopSpinner.setOnItemSelectedListener(new MyOnStopSelectedListener());
        /* 数据位 */
        binding.dataSpinner.setOnItemSelectedListener(new MyOnDataSelectedListener());
        /* 校验位 */
        binding.paritySpinner.setOnItemSelectedListener(new MyOnParitySelectedListener());
        /* 串口流控制 */
        binding.flowSpinner.setOnItemSelectedListener(new MyOnFlowSelectedListener());

        /* 清除按钮 */
        binding.clearButton.setOnClickListener(arg0 -> binding.readText.setText(""));
    }

    //打开
    private void open() {
        // 判断系统是否支持USB HOST
        if (!MyApp.driver.usbFeatureSupported()) {
            Dialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("提示")
                    .setMessage("您的手机不支持USB HOST，请更换其他手机再试！")
                    .setPositiveButton("确认", null).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            return;
        }

        //检查权限
        int rup = MyApp.driver.resumeUsbPermission();
        if (rup != 0) return;

        //usb设备列表
        rup = MyApp.driver.resumeUsbList();
        // ResumeUsbList方法用于枚举CH34X设备以及打开相关设备
        if (rup == -1) {
            Toast.makeText(MainActivity.this, "打开USB设备失败", Toast.LENGTH_SHORT).show();
            MyApp.driver.closeDevice();
            return;
        }

        //不等0就是打开设备失败
        if (rup != 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(R.drawable.icon);
            builder.setTitle("未授权限");
            builder.setMessage("确认退出吗？");
            builder.setPositiveButton("确定", (dialog, which) -> System.exit(0));
            builder.setNegativeButton("返回", null);
            builder.show();
            return;
        }

        //获取连接
        if (MyApp.driver.getUsbDeviceConnection() == null) {
            Toast.makeText(MainActivity.this, "连接设备失败!", Toast.LENGTH_SHORT).show();
            return;
        }

        //初始化
        if (!MyApp.driver.uartInit()) {//对串口设备进行初始化操作
            Toast.makeText(MainActivity.this, "初始化失败", Toast.LENGTH_SHORT).show();
            return;
        }

        //读取数据监听
        MyApp.driver.setReadListener(bytes -> {
            String text = Utils.bytesToHexString(bytes, " ");//以16进制输出
            //Log.v("长度：", "bytes.length="+ bytes.length + "\t内容：" + text);
            runOnUiThread(() -> {
                //数据
                binding.readText.append(text);
                //防止长度太长，截断
                if (binding.readText.length() > 2048) {
                    String str = binding.readText.getText().toString().substring(binding.readText.length() - 1024, binding.readText.length());
                    binding.readText.setText("");
                    binding.readText.append(str);
                }
            });
        });

        isOpen = true;
        binding.openButton.setText("点击关闭");
        binding.configButton.setEnabled(true);
        binding.writeButton.setEnabled(true);
        Toast.makeText(MainActivity.this, "USB设备已打开", Toast.LENGTH_SHORT).show();
    }

    //关闭
    private void close() {
        isOpen = false;
        binding.openButton.setText("点击打开");
        binding.configButton.setEnabled(false);
        binding.writeButton.setEnabled(false);
        MyApp.driver.closeDevice();
    }

    public class MyOnBaudSelectedListener implements OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            baudRate = Integer.parseInt(parent.getItemAtPosition(position).toString());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    public class MyOnStopSelectedListener implements OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            stopBit = (byte) Integer.parseInt(parent.getItemAtPosition(position).toString());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    public class MyOnDataSelectedListener implements OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            dataBit = (byte) Integer.parseInt(parent.getItemAtPosition(position).toString());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    public class MyOnParitySelectedListener implements OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String parityString = parent.getItemAtPosition(position).toString();
            if (parityString.compareTo("None") == 0) {
                parity = 0;
            } else if (parityString.compareTo("Odd") == 0) {
                parity = 1;
            } else if (parityString.compareTo("Even") == 0) {
                parity = 2;
            } else if (parityString.compareTo("Mark") == 0) {
                parity = 3;
            } else if (parityString.compareTo("Space") == 0) {
                parity = 4;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    public class MyOnFlowSelectedListener implements OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String flowString = parent.getItemAtPosition(position).toString();
            if (flowString.compareTo("None") == 0) {
                flowControl = 0;
            }
            if (flowString.compareTo("CTS/RTS") == 0) {
                flowControl = 1;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    @Override
    public void onDestroy() {
        isOpen = false;
        MyApp.driver.closeDevice();
        super.onDestroy();
    }
}