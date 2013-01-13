/*
 *   Copyright 2012 Shell M. Shrader
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.shellware.adaptronic.adaptive.tuner.bluetooth;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.shellware.adaptronic.adaptive.tuner.MainActivity;
import com.shellware.adaptronic.adaptive.tuner.services.ConnectionService;

    public class ConnectThread extends Thread {

    	private static final String TAG = MainActivity.TAG;
    	private static final boolean DEBUG = MainActivity.DEBUG;

    	private static final UUID UUID_RFCOMM_GENERIC = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    	private final Handler handler;
    	private final String name;
    	private final String addr;
    	private final ConnectedThread connectedThread;
    	
    	private BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
    	private BluetoothDevice btd;
    	private BluetoothSocket bts;

		private boolean cancelled;
    	
    	public ConnectThread(final Handler handler, final String deviceName, final String addr, ConnectedThread connectedThread) {
    		super();
    		this.handler = handler;
    		this.name = deviceName;
    		this.addr = addr;
    		this.connectedThread = connectedThread;
    	}
    	
		@Override
		public void run() {

	        Bundle b = new Bundle();       
	        Message msg = new Message();

	        int counter = 0;
	        
	        while (true) {
		
	        	try {            	
	    	        btd = bt.getRemoteDevice(addr);        		

	        	} catch (Exception ex) {
					// bail if cancelled
					if (cancelled) return;

					if (DEBUG) Log.d(TAG, "bluetooth adapter: " + ex.getMessage());
	        	}
		        
	        	try {
		        	if (counter < 3) {
						if (DEBUG) Log.d(TAG, "Trying createRfcommSocketToServiceRecord");
						bts = btd.createRfcommSocketToServiceRecord(UUID_RFCOMM_GENERIC);		        			
		        	} else {
		        		if (counter < 6) {
							if (DEBUG) Log.d(TAG, "Trying createInsecureRfcommSocketToServiceRecord");
							bts = btd.createInsecureRfcommSocketToServiceRecord(UUID_RFCOMM_GENERIC);		
						} else {
							if (counter < 9) {
								if (DEBUG) Log.d(TAG, "Trying createInsecureRfcommSocket");
								Method m = btd.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
								bts = (BluetoothSocket) m.invoke(btd, Integer.valueOf(1)); // 1==RFCOMM channel cod (class of device)
							} else {
								if (DEBUG) Log.d(TAG, "Trying createRfcommSocket");							
								Method m = btd.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
					            bts = (BluetoothSocket) m.invoke(btd, Integer.valueOf(1));		
							}
		        		}
		        	}
	        	} catch (Exception ex) {
					if (cancelled) return;
					if (DEBUG) Log.d(TAG, "createRfcommSocket failed: " + ex.getMessage());	        		
	        	}
		        
		        try {
		        	bt.cancelDiscovery();
					bts.connect();
					break;
				} catch (Exception e) {
					// bail if cancelled
					if (cancelled) return;
					
					counter++;
					if (DEBUG) Log.d(TAG, "BT connect failed: " + e.getMessage());
					
			        // bail if we've tried 15 times
			        if (counter >= 15) {
				        
				        b.putShort("handle", ConnectionService.CONNECTION_ERROR);
				        b.putString("title", name);
				        b.putString("message", String.format("Unable to connect to %s: %s", name.trim(), e.getMessage()));
				        msg.setData(b);
				        
				        if (DEBUG) Log.d(TAG, "Unable to connect - " + e.getMessage());
				        handler.sendMessage(msg);
				        return;
			        }
				}
	        }

    		connectedThread.setSocket(bts);
    		connectedThread.start();

	        b.putShort("handle", ConnectionService.CONNECTED);
	        b.putString("name", name);
	        b.putString("addr", addr);
	        
	        msg.setData(b);
	        
	        if (DEBUG) Log.d(TAG, "Connected");
	        handler.sendMessage(msg);
		}
		
		public void cancel() {
			cancelled = true;
			
			if (bts != null) {
				try {
					bts.close();
				} catch (IOException e) {
					// do nothing
				}
			}
			
			this.interrupt();
		}
    }