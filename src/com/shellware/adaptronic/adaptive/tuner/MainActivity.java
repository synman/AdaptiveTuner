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
package com.shellware.adaptronic.adaptive.tuner;

import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shellware.adaptronic.adaptive.tuner.bluetooth.ConnectThread;
import com.shellware.adaptronic.adaptive.tuner.bluetooth.ConnectedThread;
import com.shellware.adaptronic.adaptive.tuner.modbus.ModbusRTU;

public class MainActivity extends Activity {
	
	public static final String TAG = "Adaptive";
	public static final boolean DEBUG_MODE = false;
	
	public static final short CONNECTION_ERROR = 1;
	public static final short DATA_READY = 2;
	public static final short CONNECTED = 3;

	private static final byte SLAVE_ADDRESS = 0x01;
	private static final byte HOLDING_REGISTER = 0x03;
	
	private static final short REGISTER_4096_PLUS_FIVE = 4096;
	private static final int REGISTER_4096_LENGTH = 17;
	
	private static final short REGISTER_4140 = 4140;
	private static final short REGISTER_4146 = 4146;
	private static final int SINGLE_REGISTER_LENGTH = 7;

	private static final String SIX_REGISTERS = "1 3 C ";
	private static final String ONE_REGISTER = "1 3 2 ";
	
	private static final int SHORT_PAUSE = 125;
	private static final int LONG_PAUSE = 250;
	
//	private TextView txtData;
	private ListView lvDevices;
	private RelativeLayout layoutDevices;
	private MenuItem menuConnect;
	private GridView gridData;
	private ImageView imgStatus;
	
	private ImageView imgIWait;
	private ImageView imgIRpm;
	private ImageView imgILoad;
	
	private ImageView imgFWait;
	private ImageView imgFRpm;
	private ImageView imgFLoad;
	
	private ProgressDialog progress;
	
	private Handler refreshHandler = new Handler();
	private ConnectionHandler connectionHandler = new ConnectionHandler();
	private ConnectedThread connected;
		
	private BluetoothAdapter bt;
//	private BluetoothDevice btd;
//	private BluetoothSocket bts;
	
	private ArrayAdapter<String> devices;
	private ArrayAdapter<String> dataArray;

	private StringBuffer dataBuffer = new StringBuffer(512);
	
	private float targetAFR = 0f;
	private int lastRPM = 0;
	private short lastRegister = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
//        txtData = (TextView) findViewById(R.id.txtData);
        lvDevices = (ListView) findViewById(R.id.lvDevices);
        layoutDevices = (RelativeLayout) findViewById(R.id.layoutDevices);
        imgStatus = (ImageView) findViewById(R.id.imgStatus);
        
        imgIWait = (ImageView) findViewById(R.id.imgIWait);
		imgIRpm = (ImageView) findViewById(R.id.imgIRpm);
        imgILoad = (ImageView) findViewById(R.id.imgILoad);

        imgFWait = (ImageView) findViewById(R.id.imgFWait);
		imgFRpm = (ImageView) findViewById(R.id.imgFRpm);
        imgFLoad = (ImageView) findViewById(R.id.imgFLoad);

        gridData = (GridView) findViewById(R.id.gridData);
        dataArray = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        gridData.setAdapter(dataArray);
        
        lvDevices.setOnItemClickListener(DevicesClickListener);
    }
    
    
    @Override
	protected void onResume() {
    	super.onResume();
	}

    final Runnable RefreshRunnable = new Runnable()
    {
        public void run() 
        {
        	final String data = getDataBuffer();
        	
        	// first check that we've got the right device and mode
        	if (data.length() > 0 && data.startsWith("1 3 ")) {
        		// RPM, MAP, MAT, WAT, AUXT, & AFR - 6 16 bit integers (twelve bytes)
        		if (data.contains(SIX_REGISTERS) && data.length() >= REGISTER_4096_LENGTH) {
        			final String[] buf = data.substring(data.indexOf(SIX_REGISTERS), data.length()).split(" ");

        			if (ModbusRTU.validCRC(buf, REGISTER_4096_LENGTH)) {   
    					imgStatus.setBackgroundColor(Color.GREEN);
	            		dataArray.clear();
	            		
	            		final int rpm = Integer.parseInt(buf[3] + buf[4], 16);
		        		dataArray.add(String.format("RPM\n%d", (rpm < 200 ? lastRPM : rpm)));
		        		if (rpm >= 200) lastRPM = rpm;
		        		
		        		dataArray.add(String.format("MAP\n%d kPa", Integer.parseInt(buf[5] + buf[6], 16)));
		        		dataArray.add(String.format("MAT\n%d\u00B0 F", Integer.parseInt(buf[7] + buf[8], 16) * 9 / 5 + 32));
		        		
		        		dataArray.add("TAFR\n" +  (targetAFR != 0f ? String.format("%.1f", targetAFR) : "--.-"));
		        		dataArray.add(String.format("AFR\n%.1f (%.1f)", Integer.parseInt(buf[14], 16) / 10f, Integer.parseInt(buf[13], 16) / 10f));
		        		dataArray.add(String.format("WAT\n%d\u00B0 F", Integer.parseInt(buf[9] + buf[10], 16) * 9 / 5 + 32));
		        		
		        		if (connected != null && connected.isAlive()) {
		        			connected.write(ModbusRTU.getRegister(SLAVE_ADDRESS, HOLDING_REGISTER, REGISTER_4140)); 
			            	lastRegister = REGISTER_4140;
		        		}
		                refreshHandler.postDelayed(this, SHORT_PAUSE);

		                if (DEBUG_MODE) Log.d(TAG, "Processed 4096 response: " + data);
		                return;
        			} else {
    					if (DEBUG_MODE) Log.d(TAG, "bad CRC for " + lastRegister);
        			}
	        	} else {
	        		// Learning Flags and Target AFR - one 16 bit integer (two bytes)
	        		if (data.contains(ONE_REGISTER) && data.length() >= SINGLE_REGISTER_LENGTH) {
	        			final String[] buf = data.substring(data.indexOf("1 3 2 "), data.length()).split(" ");
 			
	        			if (ModbusRTU.validCRC(buf, SINGLE_REGISTER_LENGTH)) { 
        					imgStatus.setBackgroundColor(Color.GREEN);
	        				
	        				switch (lastRegister) {
		        				case REGISTER_4140:	// target AFR
		        					targetAFR = Integer.parseInt(buf[3] + buf[4], 16) / 10f;
		        					
					        		if (connected != null && connected.isAlive()) {
					        			connected.write(ModbusRTU.getRegister(SLAVE_ADDRESS, HOLDING_REGISTER, REGISTER_4146)); 
						            	lastRegister = REGISTER_4146;
					        		}

					        		refreshHandler.postDelayed(this, SHORT_PAUSE);	
					        		if (DEBUG_MODE) Log.d(TAG, "Processed 4140 response: " + data);
		    		                return;
		    		                
		        				case REGISTER_4146:	// learning flags
			        				imgFWait.setBackgroundColor(Color.TRANSPARENT);
			        				imgFRpm.setBackgroundColor(Color.TRANSPARENT);
			        				imgFLoad.setBackgroundColor(Color.TRANSPARENT);
			        				
			        				imgIWait.setBackgroundColor(Color.TRANSPARENT);
			        				imgIRpm.setBackgroundColor(Color.TRANSPARENT);
		        					imgILoad.setBackgroundColor(Color.TRANSPARENT);
			        				
			        				 if (getBit(Integer.parseInt(buf[3] + buf[4], 16), 0) > 0) 
			        					 imgFWait.setBackgroundColor(Color.RED);
			        				 if (getBit(Integer.parseInt(buf[3] + buf[4], 16), 1) > 0) 
			        					 imgFRpm.setBackgroundColor(Color.GREEN);
			        				 if (getBit(Integer.parseInt(buf[3] + buf[4], 16), 2) > 0) 
			        					 imgFLoad.setBackgroundColor(Color.GREEN);
		
			        				 if (getBit(Integer.parseInt(buf[3] + buf[4], 16), 3) > 0) 
			        					 imgIWait.setBackgroundColor(Color.RED);
			        				 if (getBit(Integer.parseInt(buf[3] + buf[4], 16), 4) > 0) 
			        					 imgIRpm.setBackgroundColor(Color.GREEN);
			        				 if (getBit(Integer.parseInt(buf[3] + buf[4], 16), 5) > 0) 
			        					imgILoad.setBackgroundColor(Color.GREEN);
			        				 
					        		if (connected != null && connected.isAlive()) {
					        			connected.write(ModbusRTU.getRegister(SLAVE_ADDRESS, HOLDING_REGISTER, REGISTER_4096_PLUS_FIVE, (short) 6)); 
						            	lastRegister = REGISTER_4096_PLUS_FIVE;
					        		}
					        		
					        		refreshHandler.postDelayed(this, LONG_PAUSE);
			        				 if (DEBUG_MODE) Log.d(TAG, "Processed 4146 response: " + data);
			        				 return;
			        				 
		        				default:
		        					// should never get here
		        					Log.d(TAG, "should have never got here");
	        				}
        				} else {
        					if (DEBUG_MODE) Log.d(TAG, "bad CRC for " + lastRegister);
        				}
	        		}
	        	}
        	}
        	
        	// last time slice of data is trash -- discard it and try again
        	if (DEBUG_MODE) Log.d(TAG, lastRegister + " response discarded: " + data);
			imgStatus.setBackgroundColor(Color.RED);
			
			switch (lastRegister) {
        		case REGISTER_4096_PLUS_FIVE:
	        		if (connected != null && connected.isAlive()) {
	        			connected.write(ModbusRTU.getRegister(SLAVE_ADDRESS, HOLDING_REGISTER, lastRegister, (short) 6)); 
	        		}
                    refreshHandler.postDelayed(this, LONG_PAUSE * 2);
                    break;
        		default:
	        		if (connected != null && connected.isAlive()) {
	        			connected.write(ModbusRTU.getRegister(SLAVE_ADDRESS, HOLDING_REGISTER, lastRegister)); 
	        		}
        			refreshHandler.postDelayed(this,  SHORT_PAUSE * 2);
        	}
        }
    	
	    private int getBit(final int item, final int position) {   
	    	return (item >> position) & 1;
	    }
    };
    
	private class ConnectionHandler extends Handler {

		@Override
		public void handleMessage(Message message) {
		
	        if (progress != null && progress.isShowing()) progress.dismiss();

	        switch (message.getData().getShort("handle")) {
	        	case CONNECTED: 
		    		menuConnect.setTitle(R.string.menu_disconnect);
		    		lastRegister = REGISTER_4096_PLUS_FIVE;
					connected.write(ModbusRTU.getRegister(SLAVE_ADDRESS, HOLDING_REGISTER, lastRegister, (short) 6));     		
		    		refreshHandler.postDelayed(RefreshRunnable, LONG_PAUSE);
		    		
		    		break;
		    		
	        	case CONNECTION_ERROR:
					AlertDialog alert = new AlertDialog.Builder(MainActivity.this).create();
					alert.setTitle(message.getData().getString("title"));
					alert.setMessage("\n" + message.getData().getString("message") + "\n");
					alert.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", 
							new DialogInterface.OnClickListener() {	
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							});
					alert.show();
					
					disconnect();
					break;
					
	        	case DATA_READY:
	    			byte[] data = message.getData().getByteArray("data");
	    			int length = message.getData().getInt("length");
	    			
	    			if (length > 0) setDataBuffer(data, length);			
			}
		}	
    }   
    
    private String getDataBuffer() {
    	synchronized(this) {
			final String ret = dataBuffer.toString();
			dataBuffer.setLength(0);
			return ret.trim();
    	}
    }

    private void setDataBuffer(final byte[] data, final int length) {
    	synchronized(this) {
	        for (int x = 0; x < length; x++) {
//	        	dataBuffer.append(myFormatter.format(data[x]));
	        	dataBuffer.append(String.format("%X ", data[x]));
	        }
    	}
    }
    
    private OnItemClickListener DevicesClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

    	// Get the device MAC address, which is the last 17 chars in the View
            final String[] info = ((TextView) v).getText().toString().split("\n");
            final String name = info[0];
            final String address = info[1];
            
            layoutDevices.setVisibility(View.INVISIBLE);
            connect(name, address);
    	}
    };
    
    private void showDevices() {
    	
    	try {    		
	    	if (bt == null) bt = BluetoothAdapter.getDefaultAdapter();
	    	if (devices == null) devices = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice);
	    	devices.clear();
	    	
	    	Set<BluetoothDevice> pairedDevices = bt.getBondedDevices();
	    	// If there are paired devices
	    	if (pairedDevices.size() > 0) {
	    	    // Loop through paired devices
	    	    for (BluetoothDevice device : pairedDevices) {
	    	        // Add the name and address to an array adapter to show in a ListView
	    	        devices.add(device.getName() + "\n" + device.getAddress());
	    	    }
	    	}
	    	
	    	lvDevices.setAdapter(devices);
    	} catch(Exception ex) {
    		// do nothing
    	}
    	
    	layoutDevices.setVisibility(View.VISIBLE);
    }
    
    private void connect(final String name, final String macAddr) {
    	
    	progress = ProgressDialog.show(this, "Bluetooth Connection" , "Connecting to " + name);
    	
    	connected = new ConnectedThread(connectionHandler);    	
    	ConnectThread doConnect = new ConnectThread(connectionHandler, name, macAddr, connected);
    	doConnect.start();
    }
    
    // needs to be become a handler
    private void disconnect() {
    	
    	refreshHandler.removeCallbacks(RefreshRunnable);
    	dataArray.clear();
		menuConnect.setTitle(R.string.menu_connect);
    	
		try {
	    	if (connected != null && connected.isAlive()) connected.cancel();
		} catch (Exception e) {
			// do nothing
		}	
    }
    
    @SuppressWarnings("unused")
	private void sleep(final int millis) {
    	try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// do nothing
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		
        // Handle item selection
        switch (item.getItemId()) {
	        case android.R.id.home:
	        	return false;
	        case R.id.menu_exit:
	        	System.exit(0);
	        case R.id.menu_connect:
	        	if (menuConnect == null) menuConnect = item;
	        	if (item.getTitle().toString().equalsIgnoreCase(getResources().getString(R.string.menu_connect))) {
	        		showDevices();
	        	} else {
	        		disconnect();
	        	}
	            return false;
        	default:
                return super.onOptionsItemSelected(item);
        }
    } 
}
