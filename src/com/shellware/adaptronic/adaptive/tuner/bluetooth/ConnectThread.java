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
import java.util.UUID;

import com.shellware.adaptronic.adaptive.tuner.MainActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

    public class ConnectThread extends Thread {

    	private static final UUID UUID_RFCOMM_GENERIC = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    	private final Handler handler;
    	private final String name;
    	private final String addr;
    	private final ConnectedThread connectedThread;
    	
    	private BluetoothAdapter bt;
    	private BluetoothDevice btd;
    	private BluetoothSocket bts;
    	
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
	    	        bt = BluetoothAdapter.getDefaultAdapter();
	    	        btd = bt.getRemoteDevice(addr);        		
	        	} catch (Exception ex) {
	        		// do nothing -- let it fall thru and eventually crash
	        	}
		        
		        try {
		        	bt.cancelDiscovery();
					bts = btd.createRfcommSocketToServiceRecord(UUID_RFCOMM_GENERIC);
				} catch (IOException e) {
					// try an insecure connection
					try {
						bts = btd.createInsecureRfcommSocketToServiceRecord(UUID_RFCOMM_GENERIC);
					} catch (IOException e1) {
						// increment counter
						counter++;
					}
				}
		        
		        try {
					bts.connect();
					break;
				} catch (IOException e) {
					counter++;
					
			        // bail if we've tried 10 times
			        if (counter >= 10) {
				        
				        b.putShort("handle", MainActivity.CONNECTION_ERROR);
				        b.putString("title", name);
				        b.putString("message", "Connection attempt failed");
				        msg.setData(b);
				        
				        if (MainActivity.DEBUG_MODE) Log.d(MainActivity.TAG, "Unable to connect - " + e.getMessage());
				        handler.sendMessage(msg);
				        return;
			        }
				}
	        }

    		connectedThread.setSocket(bts);
    		connectedThread.start();

	        b.putShort("handle", MainActivity.CONNECTED);
	        msg.setData(b);
	        
	        if (MainActivity.DEBUG_MODE) Log.d(MainActivity.TAG, "Connected");
	        handler.sendMessage(msg);
		}
    }