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
import java.io.InputStream;
import java.io.OutputStream;

import com.shellware.adaptronic.adaptive.tuner.MainActivity;
import com.shellware.adaptronic.adaptive.tuner.services.ConnectionService;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ConnectedThread extends Thread {
	
	private static final String TAG = MainActivity.TAG;
	private static final boolean DEBUG = MainActivity.DEBUG;

    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    
//EVAN    
//	private final Handler handler;
	protected final Handler handler;
//END EVAN
	
	private String name;
	
	protected boolean disconnecting = false;

	public ConnectedThread(Handler handler) {
        this.handler = handler;
        
    }
    
    public void setSocket(BluetoothSocket socket) {
    	mmSocket = socket;

    	try {
        	mmInStream = socket.getInputStream();
            mmOutStream = socket.getOutputStream();
        } catch (IOException e) { }
    }
 
    public void run() {
        byte[] buffer = new byte[512];  // buffer store for the stream
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception or cancel occurs
        while (!disconnecting) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);
                                    
		        if (DEBUG)  {
	                Log.d(TAG, String.format("Received %d bytes", bytes));
	                
//			        for (int x = 0; x < bytes; x++) {
//			        	Log.d(TAG, String.format("%X", buffer[x]));
//			        }
		        }
                
                // Send the obtained bytes to the UI activity
		        Bundle b = new Bundle();

		        b.putShort("handle", ConnectionService.DATA_READY);
		        b.putByteArray("data", buffer);
		        b.putInt("length", bytes);
		        
		        Message msg = new Message();
		        msg.setData(b);
		        
		        handler.sendMessage(msg);
		        
            } catch (IOException e) {
            	if (disconnecting) break;

    	        Bundle b = new Bundle();
    	        
    	        b.putShort("handle", ConnectionService.CONNECTION_ERROR);
    	        b.putString("title", name);
    	        b.putString("message", "Connection lost");
    	        
    	        Message msg = new Message();
    	        msg.setData(b);
    	        
    	        if (DEBUG) Log.d(TAG, "Connection lost on read - " + e.getMessage()); 
    	        handler.sendMessage(msg);
		        break;
            }
        }
    }
 
    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
            mmOutStream.flush();
        } catch (IOException e) { 
        	if (disconnecting) return;
        	
	        Bundle b = new Bundle();
	        
	        b.putShort("handle", ConnectionService.CONNECTION_ERROR);
	        b.putString("title", name);
	        b.putString("message", "Connection lost");
	        
	        Message msg = new Message();
	        msg.setData(b);
	        
	        if (DEBUG) Log.d(TAG, "Connection lost on write - " + e.getMessage()); 
	        handler.sendMessage(msg);
        }
    }
 
    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
    	disconnecting = true;
    	
        if (DEBUG) Log.d(TAG, "BT Connected Thread Canceled");

        if (mmInStream != null) {
            try {mmInStream.close();} catch (Exception e) {}
            mmInStream = null;
        }

        if (mmOutStream != null) {
                try {mmOutStream.close();} catch (Exception e) {}
                mmOutStream = null;
        }

        if (mmSocket != null) {
                try {mmSocket.close();} catch (Exception e) {}
                mmSocket = null;
        }
    }
}
