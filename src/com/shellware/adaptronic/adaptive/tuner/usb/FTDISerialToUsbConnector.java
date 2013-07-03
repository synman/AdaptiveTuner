package com.shellware.adaptronic.adaptive.tuner.usb;

/*
 *   Copyright 2013 Matt Waddilove
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

import android.hardware.usb.UsbDeviceConnection;

import com.shellware.adaptronic.adaptive.tuner.logging.AdaptiveLogger;

public class FTDISerialToUsbConnector extends UsbDeviceConnector {

	private static AdaptiveLogger logger = new AdaptiveLogger(AdaptiveLogger.DEFAULT_LEVEL, AdaptiveLogger.DEFAULT_TAG);

    @Override
    public String getConnectorName() {
        return "FTDI FT232R Serial -> USB Adapter";
    }

    public static final int[][] SERIAL_TO_USB_DEVICES = new int[][] {
            //Format: {USB_VENDOR_ID, USB_PRODUCT_ID}
            new int[] {0x0403, 0x6001}  // FTDI FT232R UART
    };

    @Override
    public int[][] GetSupportedDevices() {
        return SERIAL_TO_USB_DEVICES;
    }

    @Override
    public boolean InitialiseConnection(UsbDeviceConnection connection) {

    	connection.controlTransfer(0x40, 0, 0, 0, null, 0, 0);// reset
    	connection.controlTransfer(0x40, 0, 1, 0, null, 0, 0);// clear Rx
    	connection.controlTransfer(0x40, 0, 2, 0, null, 0, 0);// clear Tx
    	connection.controlTransfer(0x40, 0x02, 0x0000, 0, null, 0, 0); // flow control none
    	
        /*
         * Calculate a Divisor at 48MHz 9600 : 0x4138 11400 : 0xc107 19200 : 0x809c
         * 38400 : 0xc04e 57600 : 0x0034 115200 : 0x001a 230400 : 0x000d
         */
    	connection.controlTransfer(0x40, 0x03, 0x0034, 0, null, 0, 0);
    	
        connection.controlTransfer(0x40, 0x04, 0x0008, 0, null, 0, 0); // n/8/1
    	
    	return true;
    }
}
