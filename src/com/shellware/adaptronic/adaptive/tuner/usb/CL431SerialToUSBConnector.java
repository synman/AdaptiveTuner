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

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;

public class CL431SerialToUsbConnector extends UsbDeviceConnector {

    @Override
    public String getConnectorName() {
        return "Winchiphead CH341 Serial -> USB Adapter";
    }

    public boolean RequiresIntent() {
        return true;
    }

    public static final int[][] SERIAL_TO_USB_DEVICES = new int[][] {
            //Format: {USB_VENDOR_ID, USB_PRODUCT_ID}
            new int[] {0x1A86, 0x7523}, // Serial -> USB adapter Winchiphead CH341
            new int[] {0x4348, 0x5523}  // Serial -> USB adapter (no idea quite which device this is for, but the linux kernel driver supports it too, so might as well.
    };

    @Override
    public int[][] GetSupportedDevices() {
        return SERIAL_TO_USB_DEVICES;
    }

    @Override
    public boolean InitialiseConnection(UsbDeviceConnection connection) {
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
}
