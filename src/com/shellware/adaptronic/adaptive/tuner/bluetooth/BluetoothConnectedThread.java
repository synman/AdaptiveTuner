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
import java.util.Locale;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.shellware.adaptronic.adaptive.tuner.logging.AdaptiveLogger;
import com.shellware.adaptronic.adaptive.tuner.logging.AdaptiveLogger.Level;
import com.shellware.adaptronic.adaptive.tuner.modbus.ConnectedThread;
import com.shellware.adaptronic.adaptive.tuner.services.ConnectionService;

public class BluetoothConnectedThread extends ConnectedThread {
	
	public BluetoothConnectedThread(Handler handler) {
		super(handler);
	}

	private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;

	
	private String name;
	
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
                                    
		        AdaptiveLogger.log(String.format(Locale.US, "Received %d bytes", bytes));
                
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
    	        
    	        AdaptiveLogger.log(Level.ERROR, "Connection lost on read - " + e.getMessage()); 
    	        
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
	        
	        AdaptiveLogger.log(Level.ERROR, "Connection lost on write - " + e.getMessage()); 
	        handler.sendMessage(msg);
        }
    }
 
    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
    	disconnecting = true;
    	
        AdaptiveLogger.log("BT Connected Thread Canceled");

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
