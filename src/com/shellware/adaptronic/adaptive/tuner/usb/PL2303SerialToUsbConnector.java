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
 * INDIserver is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * INDIserver is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with INDIserver.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2012 Alexander Tuschen <atuschen75 at gmail dot com>
 */

import java.io.IOException;

import android.hardware.usb.UsbDeviceConnection;

import com.shellware.adaptronic.adaptive.tuner.logging.AdaptiveLogger;
import com.shellware.adaptronic.adaptive.tuner.logging.AdaptiveLogger.Level;

public class PL2303SerialToUsbConnector extends UsbDeviceConnector {

	private static AdaptiveLogger logger = new AdaptiveLogger(AdaptiveLogger.DEFAULT_LEVEL, AdaptiveLogger.DEFAULT_TAG);

    @Override
    public String getConnectorName() {
        return "Prolific PL2303 Serial -> USB Adapter";
    }

    public static final int[][] SERIAL_TO_USB_DEVICES = new int[][] {
            //Format: {USB_VENDOR_ID, USB_PRODUCT_ID}
            new int[] {0x4348, 0x5523}  // Serial -> USB adapter (no idea quite which device this is for, but the linux kernel driver supports it too, so might as well.
    };

    @Override
    public int[][] GetSupportedDevices() {
        return SERIAL_TO_USB_DEVICES;
    }

    @Override
    public boolean InitialiseConnection(UsbDeviceConnection connection) {

    	try {
    		
            // Initialization of PL2303 according to linux pl2303.c driver
			setup(connection, BaudRate.B57600, DataBits.D8, StopBits.S1, Parity.NONE);
            
             byte[] buffer = new byte[1];
            connection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8484, 0, buffer, 1, 100);
            connection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0x0404, 0, null, 0, 100);
            connection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8484, 0, buffer, 1, 100);
            connection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8383, 0, buffer, 1, 100);
            connection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8484, 0, buffer, 1, 100);
            connection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0x0404, 1, null, 0, 100);
            connection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8484, 0, buffer, 1, 100);
            connection.controlTransfer(VENDOR_READ_REQUEST_TYPE, VENDOR_READ_REQUEST, 0x8383, 0, buffer, 1, 100);
            connection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0, 1, null, 0, 100);
            connection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 1, 0, null, 0, 100);
            connection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 2, 0x24, null, 0, 100);
            
            logger.log("PL2303 successfully opened");
		} catch (IOException e) {
			logger.log(Level.ERROR, "unable to initialize PL2303 " + e.getMessage());
		}
        return true;

    }
    
    // Setup basic communication parameters according to linux pl2303.c driver 
    public void setup(UsbDeviceConnection connection, BaudRate R, DataBits D, StopBits S, Parity P) throws IOException {
        byte[] oldSettings = new byte[7];
        byte[] buffer = new byte[7];
        
        if (connection == null) throw new IOException("Connection closed");
        
        // Get current settings
        connection.controlTransfer(GET_LINE_REQUEST_TYPE, GET_LINE_REQUEST, 0, 0, oldSettings, 7, 100);
        
        buffer = oldSettings;
        
        // Setup Baudrate
        int baud;
        switch (R) {
        case B0: baud = 0; break;
        case B75: baud = 75; break;
        case B150: baud = 150; break;
        case B300: baud = 300; break;
        case B600: baud = 600; break;
        case B1200: baud = 1200; break;
        case B1800: baud = 1800; break;
        case B2400: baud = 2400; break;
        case B4800: baud = 4800; break;
        case B9600: baud = 9600; break;
        case B19200: baud = 19200; break;
        case B38400: baud = 38400; break;
        case B57600: baud = 57600; break;
        case B115200: baud = 115200; break;
        case B230400: baud = 230400; break;
        case B460800: baud = 460800; break;
        case B614400: baud = 614400; break;
        case B921600: baud = 921600; break;
        case B1228800: baud = 1228800; break;
        case B2457600: baud = 2457600; break;
        case B3000000: baud = 3000000; break;
        case B6000000: baud = 6000000; break;
        default: baud = 9600; break;
        }
        
        if  ((baud > 1228800) && (pl2303type == 0)) baud = 1228800; // Only PL2303HX supports higher baudrates   
        
        buffer[0]=(byte) (baud & 0xff);
        buffer[1]=(byte) ((baud >> 8) & 0xff);
        buffer[2]=(byte) ((baud >> 16) & 0xff);
        buffer[3]=(byte) ((baud >> 24) & 0xff);
        
//        // Setup Stopbits
//        switch (S) {
//        case S1: buffer[4] = 0; break;
//        case S2: buffer[4] = 2; break;
//        default: buffer[4] = 0; break;
//        }
//        
//        // Setup Parity
//        switch (P) {
//        case NONE: buffer[5] = 0; break;
//        case ODD: buffer[5] = 1; break;
//        case EVEN: buffer[5] = 2; break;
//        default: buffer[5] = 0; break;
//        }
//        
//        // Setup Databits
//        switch (D) {
//        case D5: buffer[6] = 5; break;
//        case D6: buffer[6] = 6; break;
//        case D7: buffer[6] = 7; break;
//        case D8: buffer[6] = 8; break;
//        default: buffer[6] = 8; break;
//        }

        // Set new configuration on PL2303 only if settings have changed
        //if (buffer != oldSettings) 
        connection.controlTransfer(SET_LINE_REQUEST_TYPE, SET_LINE_REQUEST, 0, 0, buffer, 7, 100); 

//        // Disable BreakControl
//        connection.controlTransfer(BREAK_REQUEST_TYPE, BREAK_REQUEST, BREAK_OFF, 0, null, 0, 100);
//        
//        // Disable FlowControl
//        connection.controlTransfer(VENDOR_WRITE_REQUEST_TYPE, VENDOR_WRITE_REQUEST, 0, 0, null, 0, 100);
        
        //TODO: implement RTS/CTS FlowControl
    }
    
    // Type 0 = PL2303, type 1 = PL2303HX
    private int pl2303type = 0;                         
    
    public enum BaudRate {
        B0, 
        B75,
        B150,
        B300,
        B600,
        B1200,
        B1800,
        B2400,
        B4800,
        B9600,
        B19200,
        B38400,
        B57600,
        B115200,
        B230400,
        B460800,
        B614400,
        B921600,
        B1228800,
        B2457600,
        B3000000,
        B6000000
    };
    
    public enum DataBits {
        D5,
        D6,
        D7,
        D8
    };
    
    public enum StopBits {
        S1,
        S2
    };
    
    public enum Parity {
        NONE,
        ODD,
        EVEN
    };
        
    // USB control commands
    private static final int SET_LINE_REQUEST_TYPE              =       0x21;
    private static final int SET_LINE_REQUEST                   =       0x20;
    private static final int BREAK_REQUEST_TYPE                 =       0x21;
    private static final int BREAK_REQUEST                              =       0x23;   
    private static final int BREAK_OFF                                  =       0x0000;
    private static final int GET_LINE_REQUEST_TYPE              =       0xa1;
    private static final int GET_LINE_REQUEST                   =       0x21;
    private static final int VENDOR_WRITE_REQUEST_TYPE  =       0x40;
    private static final int VENDOR_WRITE_REQUEST               =       0x01;
    private static final int VENDOR_READ_REQUEST_TYPE   =   0xc0;
    private static final int VENDOR_READ_REQUEST        =   0x01;
}
