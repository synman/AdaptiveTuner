package com.shellware.adaptronic.adaptivetuner.modbus;

import android.os.Handler;

abstract public class ConnectedThread extends Thread {

	abstract public void write(byte[] bytes);
	abstract public void cancel();
	abstract public void run();
	
	protected Handler handler;
	protected boolean disconnecting = false;
	
	public ConnectedThread(Handler handler) {
		this.handler = handler;
	}
}