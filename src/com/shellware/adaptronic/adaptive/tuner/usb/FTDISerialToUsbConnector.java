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
 * PORTIONS COPYRIGHT
 * 
 * Copyright 2011 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: http://code.google.com/p/usb-serial-for-android/
 */

import java.io.IOException;

import com.shellware.adaptronic.adaptive.tuner.MainActivity;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;

public class FTDISerialToUsbConnector extends UsbDeviceConnector {

	private static final String TAG = MainActivity.TAG;
	private static final boolean DEBUG = MainActivity.DEBUG;

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

    	try {
    		
            // Initialization of PL2303 according to linux pl2303.c driver
			setup(connection, 57600);
            
//             byte[] buffer = new byte[1];
//            connection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8484, 0, buffer, 1, 100);
//            connection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0x0404, 0, null, 0, 100);
//            connection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8484, 0, buffer, 1, 100);
//            connection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8383, 0, buffer, 1, 100);
//            connection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8484, 0, buffer, 1, 100);
//            connection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0x0404, 1, null, 0, 100);
//            connection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8484, 0, buffer, 1, 100);
//            connection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8383, 0, buffer, 1, 100);
//            connection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0, 1, null, 0, 100);
//            connection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 1, 0, null, 0, 100);
//            connection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 2, 0x24, null, 0, 100);
            
            if (DEBUG) Log.d(TAG, "FTDI successfully opened");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return true;

    }
    
    // Setup basic communication parameters
    public void setup(UsbDeviceConnection connection, int baud) throws IOException {
    	
    	// this is supposed to be a reset
        int result = connection.controlTransfer(FTDI_DEVICE_OUT_REQTYPE, SIO_RESET_REQUEST,
                SIO_RESET_SIO, 0 /* index */, null, 0, USB_WRITE_TIMEOUT_MILLIS);
        if (result != 0) {
            throw new IOException("FTDI Reset failed: result=" + result);
        }

        mType = DeviceType.TYPE_R;
            	
        long[] vals = convertBaudrate(baud);
//        long actualBaudrate = vals[0];
        long index = vals[1];
        long value = vals[2];
        
        // this is supposed to set the baudrate
        result = connection.controlTransfer(FTDI_DEVICE_OUT_REQTYPE,
                SIO_SET_BAUD_RATE_REQUEST, (int) value, (int) index,
                null, 0, USB_WRITE_TIMEOUT_MILLIS);
        if (result != 0) {
            throw new IOException("FTDI Setting baudrate failed: result=" + result);
        }
           
        // 8 start bits, 1 stop bit, no parity
        int config = 8;

        config |= (0x00 << 8);
        config |= (0x00 << 11);

        // this is supposed to set the start/stop bits and parity
        result = connection.controlTransfer(FTDI_DEVICE_OUT_REQTYPE,
                SIO_SET_DATA_REQUEST, config, 0 /* index */,
                null, 0, USB_WRITE_TIMEOUT_MILLIS);
        if (result != 0) {
            throw new IOException("FTDI Setting parameters failed: result=" + result);
        }
        
    }
    
	 // USB control commands
	 public static final int USB_TYPE_STANDARD = 0x00 << 5;
	 public static final int USB_TYPE_CLASS = 0x00 << 5;
	 public static final int USB_TYPE_VENDOR = 0x00 << 5;
	 public static final int USB_TYPE_RESERVED = 0x00 << 5;
	
	 public static final int USB_RECIP_DEVICE = 0x00;
	 public static final int USB_RECIP_INTERFACE = 0x01;
	 public static final int USB_RECIP_ENDPOINT = 0x02;
	 public static final int USB_RECIP_OTHER = 0x03;
	
	 public static final int USB_ENDPOINT_IN = 0x80;
	 public static final int USB_ENDPOINT_OUT = 0x00;
	
	 public static final int USB_WRITE_TIMEOUT_MILLIS = 5000;
	 public static final int USB_READ_TIMEOUT_MILLIS = 5000;
	 
    // From ftdi.h
    /**
     * Reset the port.
     */
    private static final int SIO_RESET_REQUEST = 0;

    /**
     * Set the modem control register.
     */
    private static final int SIO_MODEM_CTRL_REQUEST = 1;

    /**
     * Set flow control register.
     */
    private static final int SIO_SET_FLOW_CTRL_REQUEST = 2;

    /**
     * Set baud rate.
     */
    private static final int SIO_SET_BAUD_RATE_REQUEST = 3;

    /**
     * Set the data characteristics of the port.
     */
    private static final int SIO_SET_DATA_REQUEST = 4;

    private static final int SIO_RESET_SIO = 0;

    public static final int FTDI_DEVICE_OUT_REQTYPE =
            UsbConstants.USB_TYPE_VENDOR | USB_RECIP_DEVICE | USB_ENDPOINT_OUT;

    public static final int FTDI_DEVICE_IN_REQTYPE =
            UsbConstants.USB_TYPE_VENDOR | USB_RECIP_DEVICE | USB_ENDPOINT_IN;

    /**
     * Length of the modem status header, transmitted with every read.
     */
    private static final int MODEM_STATUS_HEADER_LENGTH = 2;

    private DeviceType mType;

    /**
     * FTDI chip types.
     */
    private static enum DeviceType {
        TYPE_BM, TYPE_AM, TYPE_2232C, TYPE_R, TYPE_2232H, TYPE_4232H;
    }
    
    private long[] convertBaudrate(int baudrate) {
        // TODO(mikey): Braindead transcription of libfti method.  Clean up,
        // using more idiomatic Java where possible.
        int divisor = 24000000 / baudrate;
        int bestDivisor = 0;
        int bestBaud = 0;
        int bestBaudDiff = 0;
        int fracCode[] = {
                0, 3, 2, 4, 1, 5, 6, 7
        };

        for (int i = 0; i < 2; i++) {
            int tryDivisor = divisor + i;
            int baudEstimate;
            int baudDiff;

            if (tryDivisor <= 8) {
                // Round up to minimum supported divisor
                tryDivisor = 8;
            } else if (mType != DeviceType.TYPE_AM && tryDivisor < 12) {
                // BM doesn't support divisors 9 through 11 inclusive
                tryDivisor = 12;
            } else if (divisor < 16) {
                // AM doesn't support divisors 9 through 15 inclusive
                tryDivisor = 16;
            } else {
                if (mType == DeviceType.TYPE_AM) {
                    // TODO
                } else {
                    if (tryDivisor > 0x1FFFF) {
                        // Round down to maximum supported divisor value (for
                        // BM)
                        tryDivisor = 0x1FFFF;
                    }
                }
            }

            // Get estimated baud rate (to nearest integer)
            baudEstimate = (24000000 + (tryDivisor / 2)) / tryDivisor;

            // Get absolute difference from requested baud rate
            if (baudEstimate < baudrate) {
                baudDiff = baudrate - baudEstimate;
            } else {
                baudDiff = baudEstimate - baudrate;
            }

            if (i == 0 || baudDiff < bestBaudDiff) {
                // Closest to requested baud rate so far
                bestDivisor = tryDivisor;
                bestBaud = baudEstimate;
                bestBaudDiff = baudDiff;
                if (baudDiff == 0) {
                    // Spot on! No point trying
                    break;
                }
            }
        }

        // Encode the best divisor value
        long encodedDivisor = (bestDivisor >> 3) | (fracCode[bestDivisor & 7] << 14);
        // Deal with special cases for encoded value
        if (encodedDivisor == 1) {
            encodedDivisor = 0; // 3000000 baud
        } else if (encodedDivisor == 0x4001) {
            encodedDivisor = 1; // 2000000 baud (BM only)
        }

        // Split into "value" and "index" values
        long value = encodedDivisor & 0xFFFF;
        long index;
        if (mType == DeviceType.TYPE_2232C || mType == DeviceType.TYPE_2232H
                || mType == DeviceType.TYPE_4232H) {
            index = (encodedDivisor >> 8) & 0xffff;
            index &= 0xFF00;
            index |= 0 /* TODO mIndex */;
        } else {
            index = (encodedDivisor >> 16) & 0xffff;
        }

        // Return the nearest baud rate
        return new long[] {
                bestBaud, index, value
        };
    }
    
}
