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

import java.util.HashMap;
import java.util.Iterator;

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

public class UsbConnectedThread extends ConnectedThread {
	
	private static final String TAG = MainActivity.TAG;
	private static final boolean DEBUG = MainActivity.DEBUG;

	public static final int USB_VENDOR_ID 	= 0;
	public static final int USB_PRODUCT_ID 	= 1;

    public static final UsbDeviceConnector[] SUPPORTED_DEVICES = new UsbDeviceConnector[] {
      new SelectECUConnector(), // Select ECU
      new CL431SerialToUsbConnector()
    };

	private static UsbManager mUsbManager = null;
	
	private UsbDevice mUsbDevice = null;
	private UsbDeviceConnection mConnection = null;
	private UsbEndpoint mInEndpoint = null;
	private UsbEndpoint mOutEndpoint = null;
	
	public UsbConnectedThread(Handler handler, UsbDevice device, UsbDeviceConnection connection, UsbEndpoint inEndpoint, UsbEndpoint outEndpoint) {
		super(handler);
		
		mUsbDevice = device;
		mConnection = connection;
		mInEndpoint = inEndpoint;
		mOutEndpoint = outEndpoint;
		
		if (DEBUG) Log.d(TAG, "UsbConnectedThread created");
		
		start();
	}

	public static UsbConnectedThread checkConnectedUsbDevice(Context context, Handler handler) {
		if (mUsbManager == null) {
			mUsbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
		}
		
		if (DEBUG) Log.d(TAG, "Checking for connected USB devices");
		
    	HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
    	Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
    	
    	while (deviceIterator.hasNext()) {
			UsbDevice device = deviceIterator.next();
			
			if (DEBUG) Log.d(TAG, "Found USB device");
			
			try {

                UsbDeviceConnector recognisedDeviceConnector = null;

                for (UsbDeviceConnector deviceConnector: SUPPORTED_DEVICES) {
                    if (recognisedDeviceConnector == null) {
                        for (int[] deviceVendorProductID : deviceConnector.GetSupportedDevices()) {
                            if (device.getVendorId() == deviceVendorProductID[USB_VENDOR_ID] && device.getProductId() == deviceVendorProductID[USB_PRODUCT_ID]) {
                                recognisedDeviceConnector = deviceConnector;
                                break;
                            }
                        }
                    }
                }
			
				if (recognisedDeviceConnector != null) {
					if (DEBUG) Log.d(TAG, String.format("%s recognised", recognisedDeviceConnector.getConnectorName()));
					
					UsbDeviceConnection connection = mUsbManager.openDevice(device);

                    if (!connection.claimInterface(device.getInterface(0), true)) {
						connectionError(device.getDeviceName(), "Could not claim device interface", handler);
						return null;
					}

                    recognisedDeviceConnector.InitialiseConnection(connection);
					
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
							return new UsbConnectedThread(handler, device, connection, inEndpoint, outEndpoint);
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
