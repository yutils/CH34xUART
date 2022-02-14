# CH340/CH341的USB转串口安卓免驱应用库

### 在原版的基础上修复bug，优化代码
1. 修复：退出时，读取线程未关闭的bug
2. 新增USB断开监听回调
3. 读取数据后采用回调给前端，而不是之前的死循环
3. 采用java8
4. 支持到android12
5. demo启用dataBinding，而不再使用findViewById
6. 优化代码逻辑，使得更容易理解和使用
7. 新增大量注释（中文）
8. 更新UI布局和颜色调整

### 界面截图
![截图](doc/usb.png)  

[CH340/CH341的USB转串口安卓免驱应用库](http://www.wch.cn/download/CH341SER_ANDROID_ZIP.html)  
感谢本源码提供：[这儿](https://github.com/HeHongdan/CH34xUART)  

如果调用安卓原生串口，请参考这个工程：  
[YSerialPort](https://github.com/yutils/YSerialPort)