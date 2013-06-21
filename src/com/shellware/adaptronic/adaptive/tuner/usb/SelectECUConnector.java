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

public class SelectECUConnector extends UsbDeviceConnector {

    @Override
    public String getConnectorName() {
        return "Select ECU";
    }

    //Known ECUs
    protected static final int[][] ADAPTRONIC_USB_ECUS = new int[][] {
            //Format: {USB_VENDOR_ID, USB_PRODUCT_ID}
            new int[] {0x10C4, 0x858D} //Select ECU
    };

    @Override
    public int[][] GetSupportedDevices() {
        return ADAPTRONIC_USB_ECUS;
    }

    @Override
    public boolean InitialiseConnection(UsbDeviceConnection connection) {

        //Control codes for Silicon Labs CP201x USB to UART @ 250000 baud
        connection.controlTransfer(0x40, 0x00, 0xff, 0xff, null, 0, 0);
        connection.controlTransfer(0x40, 0x01, 0x00, 0x02, null, 0, 0);
        connection.controlTransfer(0x40, 0x01, 0x0f, 0x00, null, 0, 0);

        return true;
    }

}
