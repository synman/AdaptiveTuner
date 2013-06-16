/*
 *   Copyright 2012 Evan H. Dekker
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
 *
 *
 * ----------------------------------------------------------------------
 * 
 * USB API COMPATABILITY NOTE:
 * Tested on Acer A500 with Android 4.0.3
 * 
 * Not all Android devices that claim to support the USB Host API introduced
 * in Android 3.1 actually do.
 * 
 * For a guide, please see this list: http://usbhost.chainfire.eu/
 * If your device is not listed, please try this app: https://play.google.com/store/apps/details?id=eu.chainfire.usbhostdiagnostics
 * 
 * (Author has no affiliation with this service or app)
 *
 */
package com.shellware.adaptronic.adaptive.tuner.usb;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.shellware.adaptronic.adaptive.tuner.MainActivity;
import com.shellware.adaptronic.adaptive.tuner.bluetooth.ConnectedThread;
import com.shellware.adaptronic.adaptive.tuner.services.ConnectionService;

import java.util.HashMap;
import java.util.Iterator;

public class SerialUsbConnectedThread extends ConnectedThread {

	private static final String TAG = MainActivity.TAG;
	private static final boolean DEBUG = MainActivity.DEBUG;

	public static final int USB_VENDOR_ID 	= 0;
	public static final int USB_PRODUCT_ID 	= 1;

	//Known ECUs
	public static final int[][] SERIAL_TO_USB_DEVICES = new int[][] {
		//Format: {USB_VENDOR_ID, USB_PRODUCT_ID}
        new int[] {0x1A86, 0x7523}, // Serial -> USB adapter
        new int[] {0x4348, 0x5523}  // Serial -> USB adapter (no idea quite which device these are for, but the linux kernel driver supports it too, so might as well.
	};

	private static UsbManager mUsbManager = null;

	private UsbDevice mUsbDevice = null;
	private UsbDeviceConnection mConnection = null;
	private UsbEndpoint mInEndpoint = null;
	private UsbEndpoint mOutEndpoint = null;

	public SerialUsbConnectedThread(Handler handler, UsbDevice device, UsbDeviceConnection connection, UsbEndpoint inEndpoint, UsbEndpoint outEndpoint) {
		super(handler);
		
		mUsbDevice = device;
		mConnection = connection;
		mInEndpoint = inEndpoint;
		mOutEndpoint = outEndpoint;
		
		if (DEBUG) Log.d(TAG, "SerialUsbConnectedThread created");
		
		start();
	}

    public static boolean SetSerialControlParameters(UsbDeviceConnection connection) {
        final int USB_CONTROL_OUT = UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_DIR_OUT;
        final int USB_CONTROL_IN = UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_DIR_IN;

        // horrific magic numbers gleaned from calculations based on the Linux kernel driver source.
        // look for ch341.c, All the repetition of sending etc... is also based on the behaviour of ch341.c

        //configure CH341:
        byte buffer[] = new byte[8];
        connection.controlTransfer(USB_CONTROL_IN,  0x5f, 0x0000, 0x0000, buffer, 8, 0); //0x27 0x00
        connection.controlTransfer(USB_CONTROL_OUT, 0xa1, 0x0000, 0x0000, null, 0, 0);

        // set the baud rate to 57600 calculations used ch341_set_baudrate
        connection.controlTransfer(USB_CONTROL_OUT, 0x9a, 0x1312, 0x9803, null, 0, 0);
        connection.controlTransfer(USB_CONTROL_OUT, 0x9a, 0x0f2c, 0x0010, null, 0, 0);

        connection.controlTransfer(USB_CONTROL_IN,  0x95, 0x2518, 0x0000, buffer, 8, 0); //0x56 0x00
        connection.controlTransfer(USB_CONTROL_OUT, 0x95, 0x2518, 0x0050, null, 0, 0);

        //Get Status:
        connection.controlTransfer(USB_CONTROL_IN,  0x95, 0x0706, 0x0000, buffer, 8, 0); //0xff 0xee

        connection.controlTransfer(USB_CONTROL_OUT, 0xa1, 0x501f, 0xd90a, null, 0, 0);

        // set the baud rate to 57600 calculations used ch341_set_baudrate
        connection.controlTransfer(USB_CONTROL_OUT, 0x9a, 0x1312, 0x9803, null, 0, 0);
        connection.controlTransfer(USB_CONTROL_OUT, 0x9a, 0x0f2c, 0x0010, null, 0, 0);

        // handshake:
        connection.controlTransfer(USB_CONTROL_OUT, 0xa4, 0x00ff, 0x0000, null, 0, 0); // or maybe 0xffff?

        // Adaptronic would like 8-N-1, however there's no data on how to set it, the device defaults to 8n1, so hopefully it'll be OK :/

        //Get Status:
        connection.controlTransfer(USB_CONTROL_IN,  0x95, 0x0706, 0x0000, buffer, 8, 0); //0x9f 0xee

        // handshake:
        connection.controlTransfer(USB_CONTROL_OUT, 0xa4, 0x00ff, 0x0000, null, 0, 0); // or maybe 0xffff?

        // set the baud rate to 57600 calculations used ch341_set_baudrate
        connection.controlTransfer(USB_CONTROL_OUT, 0x9a, 0x1312, 0x9803, null, 0, 0);
        connection.controlTransfer(USB_CONTROL_OUT, 0x9a, 0x0f2c, 0x0010, null, 0, 0);

        return true;
    }


	public static SerialUsbConnectedThread checkConnectedUsbDevice(Context context, Handler handler) {
		if (mUsbManager == null) {
			mUsbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
		}
		
		if (DEBUG) Log.d(TAG, "Checking for connected Serial->USB devices");
		
    	HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
    	Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
    	
    	while (deviceIterator.hasNext()) {
			UsbDevice device = deviceIterator.next();
			
			if (DEBUG) Log.d(TAG, "Found USB device");
			
			try {
				boolean deviceReconised = false;
				
				for (int[] deviceVendorProductID : SERIAL_TO_USB_DEVICES) {
					if (device.getVendorId() == deviceVendorProductID[USB_VENDOR_ID] && device.getProductId() == deviceVendorProductID[USB_PRODUCT_ID]) {
						deviceReconised = true;
					}
				}
			
				if (deviceReconised) {
					if (DEBUG) Log.d(TAG, "Serial->USB device recognised");

                    PendingIntent pi = PendingIntent.getActivity(context, 0,
                            new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
                    mUsbManager.requestPermission(device, pi );


                    UsbDeviceConnection connection = mUsbManager.openDevice(device);
					
					if (!connection.claimInterface(device.getInterface(0), true)) {
						connectionError(device.getDeviceName(), "Could not claim device interface", handler);
						return null;
					}
					
					//Control codes for Silicon Labs CP201x USB to UART @ 250000 baud
/*					connection.controlTransfer(0x40, 0x00, 0xff, 0xff, null, 0, 0);
					connection.controlTransfer(0x40, 0x01, 0x00, 0x02, null, 0, 0);
					connection.controlTransfer(0x40, 0x01, 0x0f, 0x00, null, 0, 0);*/

                    SetSerialControlParameters(connection);
					
					UsbEndpoint inEndpoint = null;
					UsbEndpoint outEndpoint = null;
					
					for (int interfaceIndex = 0; interfaceIndex < device.getInterfaceCount(); interfaceIndex++) {
						UsbInterface usbInterface = device.getInterface(interfaceIndex);
						
						for (int index = 0; index < usbInterface.getEndpointCount(); index++) {				
							UsbEndpoint endpoint = usbInterface.getEndpoint(index);
							
							if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
								if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
									if (DEBUG) Log.d(TAG, "In Endpoint Found");	
									inEndpoint = endpoint;
								} else if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
									if (DEBUG) Log.d(TAG, "Out Endpoint Found");
									outEndpoint = endpoint;
								}
							}
						}
						
						if (inEndpoint != null && outEndpoint != null) {
							return new SerialUsbConnectedThread(handler, device, connection, inEndpoint, outEndpoint);
						}
					}
				}
            } catch (Exception ex) {
				connectionError(device.getDeviceName(), ex.getMessage(), handler);
				return null;
			}
    	}
		
		return null;
	}
	
	public void run() {
		byte[] buffer = new byte[512];
		int bytes;
		
		while (!disconnecting) {
			bytes = mConnection.bulkTransfer(mInEndpoint, buffer, buffer.length, 5);
			
			if (bytes == -1) {
//				if (DEBUG) Log.d(TAG, "Bulk Transfer In Error");
			} else {
		        if (DEBUG)  {
	                Log.d(TAG, String.format("Received %d bytes", bytes));
	                
//			        for (int x = 0; x < bytes; x++) {
//			        	Log.d(TAG, String.format("%X", buffer[x]));
//			        }
		        }
                
                // Send the obtained bytes to the connection service
		        Bundle b = new Bundle();

		        b.putShort("handle", ConnectionService.DATA_READY);
		        b.putByteArray("data", buffer);
		        b.putInt("length", bytes);
		        
		        Message msg = new Message();
		        msg.setData(b);
		        
		        handler.sendMessage(msg);
			}
			
			try {
				Thread.sleep(5);
			} catch (Exception e) {}
	
		}		
	}
	
	public void write(byte[] bytes) {
		if (mConnection.bulkTransfer(mOutEndpoint, bytes, bytes.length, 5) == -1) {
			if (DEBUG) Log.d(TAG, "Bulk Transfer Out Error");
		} else {
			if (DEBUG) Log.d(TAG, "Bulk Transfer Out Successful");
		}
	}
	
	public void cancel() {
		disconnecting = true;
	}
	
	public boolean isUsbDevice(UsbDevice usbDevice) {
		return mUsbDevice.getDeviceId() == usbDevice.getDeviceId();
	}
	
	private static void connectionError(String name, String message, Handler handler) {
        Bundle b = new Bundle();
        
        b.putShort("handle", ConnectionService.CONNECTION_ERROR);
        b.putString("title", name);
        b.putString("message", message);
        
        Message msg = new Message();
        msg.setData(b);
        
        if (DEBUG) Log.d(TAG, "Connection error - " + message); 
        handler.sendMessage(msg);
	}
}
