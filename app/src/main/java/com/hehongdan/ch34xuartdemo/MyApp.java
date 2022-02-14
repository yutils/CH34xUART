package com.hehongdan.ch34xuartdemo;

import android.annotation.SuppressLint;
import android.app.Application;

import com.hehongdan.ch34xuartdriver.CH34xUARTDriver;

public class MyApp extends Application {
	@SuppressLint("StaticFieldLeak")
	public static CH34xUARTDriver driver;//全局
}
