package com.shellware.adaptronic.adaptivetuner.usb;

import android.hardware.usb.UsbDeviceConnection;

/**
 * Created by Matt on 16/06/13.
 */
abstract public class UsbDeviceConnector {

    abstract public String getConnectorName();

    abstract public int[][] GetSupportedDevices();

    abstract public boolean InitialiseConnection(UsbDeviceConnection connection);
}
