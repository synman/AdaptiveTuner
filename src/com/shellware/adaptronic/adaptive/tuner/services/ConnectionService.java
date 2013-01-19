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
package com.shellware.adaptronic.adaptive.tuner.services;

import java.lang.ref.WeakReference;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.shellware.adaptronic.adaptive.tuner.MainActivity;
import com.shellware.adaptronic.adaptive.tuner.R;
import com.shellware.adaptronic.adaptive.tuner.bluetooth.ConnectThread;
import com.shellware.adaptronic.adaptive.tuner.bluetooth.ConnectedThread;
import com.shellware.adaptronic.adaptive.tuner.modbus.ModbusRTU;
import com.shellware.adaptronic.adaptive.tuner.usb.UsbConnectedThread;
import com.shellware.adaptronic.adaptive.tuner.valueobjects.LogItems;

public class ConnectionService extends Service {
    
    // indicates the state our service:
    public enum State {
        CONNECTING,
        CONNECTED_USB,
        CONNECTED_BT,
        DISCONNECTED
    };

    public final static String  	ACTION_CONNECT_BT = "com.shellware.adaptronic.adaptive.tuner.action.CONNECT_BT";
    public final static String 	   ACTION_CONNECT_USB = "com.shellware.adaptronic.adaptive.tuner.action.CONNECT_USB";
    public final static String  	ACTION_DISCONNECT = "com.shellware.adaptronic.adaptive.tuner.action.DISCONNECT";
    public final static String 	   ACTION_UI_INACTIVE = "com.shellware.adaptronic.adaptive.tuner.action.UI_INACTIVE";
    public final static String   	 ACTION_UI_ACTIVE = "com.shellware.adaptronic.adaptive.tuner.action.UI_ACTIVE";
    public final static String ACTION_UPDATE_FUEL_MAP = "com.shellware.adaptronic.adaptive.tuner.action.UPDATE_FUEL_MAP";

	public static final short CONNECTION_ERROR = 1;
	public static final short DATA_READY = 2;
	public static final short CONNECTED = 3;
	
	private static final String TAG = MainActivity.TAG;
	private static final boolean DEBUG = MainActivity.DEBUG;
	
	private static final byte SLAVE_ADDRESS = 0x01;
	private static final byte HOLDING_REGISTER = 0x03;
	
	private static final short REGISTER_4096_PLUS_SEVEN = 4096;
	private static final int REGISTER_4096_LENGTH = 21;
	
	private static final short REGISTER_4140_PLUS_SIX = 4140;
	private static final int REGISTER_4140_LENGTH = 19;

	private static final String DEVICE_ADDR_AND_MODE_HEADER = "01 03 ";
	private static final String SEVEN_REGISTERS = "01 03 0E ";
	private static final String EIGHT_REGISTERS = "01 03 10 ";
	private static final String SIXTEEN_REGISTERS = "01 03 20";
	
	private static final int LONG_PAUSE = 100;
	private static final int NOTIFICATION_ID = 1;
	
	private static boolean mapMode = false;
	private static short mapOffset = 0;
	private final static StringBuffer mapData = new StringBuffer(1280);
	
	private final static LogItems afrAlarmLogItems = new LogItems();
	private final static LogItems logAllItems = new LogItems();

	private final Notification notifier = new Notification();
	private static SharedPreferences prefs;
	
	private static final Handler refreshHandler = new Handler();
	private final ConnectionHandler connectionHandler = new ConnectionHandler(this);
	
	private static ConnectedThread connectedThread;
	private static ConnectThread connectThread;
	
    public static State state = State.DISCONNECTED;	
	private static boolean UI_THREAD_IS_ACTIVE = false;

	private static long updatesReceived = 0;
	private static long totalTimeMillis = 0;
	
	private static short lastRegister = 4096;
	private static long lastUpdateInMillis = 0;
	private static boolean dataNotAvailable = true;
	
	private static int tempUomPref = 1;
	private static boolean wakeLock = true;
	private static boolean afrAlarmLogging = false;
	private static float afrNotEqualTargetTolerance = 5f;
	
	private static boolean logAll = false;

	private static StringBuffer dataBuffer = new StringBuffer(512);
	private static LogItems.LogItem logItem;

	private PowerManager pm;
	private WakeLock wl;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return new ServiceBinder();
	}
	
    public class ServiceBinder extends Binder {
        public ConnectionService getService() {
            return ConnectionService.this;
        }
    }

	@Override
	public void onCreate() {
		super.onCreate();

        notifier.icon = R.drawable.ic_launcher_gs;
//        notifier.largeIcon = convertToGrayscale(getResources().getDrawable(R.drawable.ic_launcher));
//        notifier.largeIcon = ((BitmapDrawable) getResources().getDrawable(R.drawable.ic_launcher)).getBitmap();
        
        notifier.flags |= Notification.FLAG_ONGOING_EVENT;
        notifier.tickerText = getResources().getString(R.string.service_name);

    	prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	
    	// initialize our logger
    	logItem = new LogItems().newLogItem();
    	
    	tempUomPref = Integer.parseInt(prefs.getString("prefs_uom_temp", "1"));
    	wakeLock = prefs.getBoolean("prefs_wake_lock", true);
    	afrAlarmLogging = prefs.getBoolean("prefs_afr_alarm_logging", false); 
    	afrNotEqualTargetTolerance = prefs.getFloat("prefs_afrnottarget_tolerance_pref", 5f);
	    
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK + PowerManager.ON_AFTER_RELEASE, getResources().getString(R.string.app_name));
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		final String action = intent.getAction();
		
		// bail if no intent 
		if (action == null) return START_NOT_STICKY;
		if (DEBUG) Log.d(TAG, "service start intent: " + action);

        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        
        // initiate a bluetooth connection
        if (action.equals(ACTION_CONNECT_BT)) {
        	final String name = intent.getStringExtra("name");
        	final String addr = intent.getStringExtra("addr");
        	
        	state = State.CONNECTING;
            notifier.tickerText = String.format(getResources().getString(R.string.service_connecting), name);
            
        	connectedThread = new ConnectedThread(connectionHandler);    	
        	connectThread = new ConnectThread(connectionHandler, name, addr, connectedThread);
        	connectThread.start();
        }
        
        // initiate a USB connection
        if (action.equals(ACTION_CONNECT_USB)) {
			final UsbConnectedThread usbThread = UsbConnectedThread.checkConnectedUsbDevice(getApplicationContext(), connectionHandler);
			
			if (usbThread != null) {
				connectedThread = usbThread;
				
				lastUpdateInMillis = System.currentTimeMillis();	            
	    		totalTimeMillis = 0;
	    		updatesReceived = 0;
				
	        	state = State.CONNECTED_USB;
	            notifier.tickerText = getResources().getString(R.string.service_usb_connected);

	            sendRequest(REGISTER_4096_PLUS_SEVEN);

	    		refreshHandler.postDelayed(RefreshRunnable, LONG_PAUSE);

	            // acquire wakelock
	            if (wakeLock && !wl.isHeld()) wl.acquire();
			}
        }
        
        // map mode
        if (action.equals(ACTION_UPDATE_FUEL_MAP)) {
        	mapMode = true;
        	mapOffset = 0;
        	mapData.setLength(0);
        	
        	sendRequest(mapOffset);
        }
        
        // disconnect
        if (action.equals(ACTION_DISCONNECT)) {
            notifier.tickerText = getResources().getString(R.string.service_disconnected); 
            disconnect();
        }
        
        // indicate UI thread is alive
        if (action.equals(ACTION_UI_ACTIVE)) {
        	UI_THREAD_IS_ACTIVE = true;
        	stopForeground(true);
        }
        
        // indicate UI thread has been shutdown
        if (action.equals(ACTION_UI_INACTIVE)) {
        	UI_THREAD_IS_ACTIVE = false;            
        }
        
        // show a notification if our activity doesn't have focus
        if (!UI_THREAD_IS_ACTIVE) {
            notifier.setLatestEventInfo(getApplicationContext(), getString(R.string.app_name), notifier.tickerText, pi);     
            startForeground(NOTIFICATION_ID, notifier);
        }
        
        return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		disconnect();
		
		stopForeground(true);
		state = State.DISCONNECTED;
		
		if (DEBUG) Log.d(TAG, "service destroy");
	}
	
	private final static Runnable RefreshRunnable = new Runnable() {

		public void run() {

        	// last time slice of data is trash -- discard it and try again
        	if (System.currentTimeMillis() - lastUpdateInMillis > LONG_PAUSE) {
        		if (DEBUG) Log.d(TAG, lastRegister + " response timed out: " + dataBuffer.toString());

        		sendRequest();
				
        		dataNotAvailable = true;
        	}
        	
    		refreshHandler.postDelayed(this, LONG_PAUSE);
        }
    };
    
	private static class ConnectionHandler extends Handler {
		
		private final WeakReference<ConnectionService> theService;

		ConnectionHandler(ConnectionService service) {
			theService = new WeakReference<ConnectionService>(service);
		}

		@Override
		public void handleMessage(Message message) {
			
			ConnectionService service = theService.get();
			if (service == null) {
				if (DEBUG) Log.d(TAG, "service reference is null");
				return;
			}
		
	        switch (message.getData().getShort("handle")) {
	        	case CONNECTED:
	        		// this message is only sent via BT connect
	        		final String name = message.getData().getString("name");
	        		final String addr = message.getData().getString("addr");
	        		
	        		service.connect(name, addr);
		    		break;
		    		
	        	case CONNECTION_ERROR:
					service.disconnect();
					break;
					
	        	case DATA_READY:
	    			byte[] msg = message.getData().getByteArray("data");
	    			int msglength = message.getData().getInt("length");
	    			
	    			if (msglength > 0) { 
	    	        	final String data = setDataBuffer(msg, msglength).toString();
	    	        	
	    	        	// this could be a lot more efficient -- rethink all the data type conversions
	    	        	final int datalength = data.trim().split(" ").length;
	    	        	
	    	        	// first check that we've got the right device and mode
	    	        	if (datalength > 2 && data.contains(DEVICE_ADDR_AND_MODE_HEADER)) {
	    	        		
	    	        		// if we're in map mode process any map data if it exists
	    	        		if (mapMode) {
	    	        			if (data.contains(SIXTEEN_REGISTERS) && datalength >= 37 && ModbusRTU.validCRC(data.trim().split(" "), 37)) {
	    	        				populateFuelMap(data);
	    	        			}
	    	        			break;
	    	        		}
	    	        		
	    	        		// do we have a bad message?
	    	        		if (!(data.contains(EIGHT_REGISTERS) || data.contains(SEVEN_REGISTERS))) {
	    	            		if (DEBUG) Log.d(TAG, lastRegister + " response discarded: " + data);
	    	            		dataNotAvailable = true;
	    	        			sendRequest();
	    	        			break;
	    	        		}
	    	        		
	    	        		// RPM, MAP, MAT, WAT, AUXT, AFR, TPS - 8 16 bit integers (sixteen bytes)
	    	        		if (data.contains(EIGHT_REGISTERS) && datalength >= REGISTER_4096_LENGTH) {
	    	        			process4096Response(data);
	    		        	} else {
    		        			if (data.contains(SEVEN_REGISTERS) && datalength >= REGISTER_4140_LENGTH) {
    		        				process4140Response(data);
    		        			}
	    		        	}
	    	        	}
    	        	}
			}
		}	
    }
	
	private static void populateFuelMap(final String data) {

		mapOffset+=16;

		String[] mapmap = data.trim().split(" ");
		
		for (int x = 3; x < 35; x++) {
			mapData.append(mapmap[x]);
			mapData.append(" ");
		}

		if (mapOffset < 512) {
			sendRequest(mapOffset);
			return;
		}

		MainActivity.mapReady = true;

		lastRegister = REGISTER_4096_PLUS_SEVEN;
		mapMode = false;		
	}
	

    private static void sendRequest() {
    	sendRequest(lastRegister);
    }
	private static void sendRequest(final short register) {
    	
    	final long currentTimeInMillis = System.currentTimeMillis();
    	final long elapsed = currentTimeInMillis - lastUpdateInMillis;
    	totalTimeMillis+=elapsed;
    	updatesReceived++;
    	
     	clearDataBuffer();
    	
    	short length;
    	
    	switch (register) {
    		case REGISTER_4096_PLUS_SEVEN:
    			length = 8;
    			break;
    		case REGISTER_4140_PLUS_SIX:
    			length = 7;
    			break;
			default:
				length = 16;
				break;
    	}
    
		if (connectedThread != null && connectedThread.isAlive()) {
			connectedThread.write(ModbusRTU.getRegister(SLAVE_ADDRESS, HOLDING_REGISTER, register, length));
			
	    	lastUpdateInMillis = currentTimeInMillis;
	    	lastRegister = register;
		}
	}
	
	private void connect(final String name, final String addr) {
		state = state == State.CONNECTING ? State.CONNECTED_BT : State.CONNECTED_USB;

		sendRequest(REGISTER_4096_PLUS_SEVEN);
		totalTimeMillis = 0;
		updatesReceived = 0;

		refreshHandler.postDelayed(RefreshRunnable, LONG_PAUSE);

		Editor edit = prefs.edit();
		
		edit.putString("prefs_remote_name", name);
		edit.putString("prefs_remote_mac", addr);
		edit.commit();

		notifier.tickerText = String.format(getResources().getString(R.string.service_bt_connected), name);
		
        // acquire wakelock
        if (wakeLock && !wl.isHeld()) wl.acquire();

        if (!UI_THREAD_IS_ACTIVE) {
            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                    new Intent(getApplicationContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

            notifier.setLatestEventInfo(getApplicationContext(), getString(R.string.app_name), notifier.tickerText, pi);        
        }
	}
	
    private void disconnect() {
    	refreshHandler.removeCallbacks(RefreshRunnable);			
    	
		try {
	    	if (connectedThread != null && connectedThread.isAlive()) {
	    		connectedThread.cancel();
	    		connectedThread.join();
	    	}
		} catch (Exception e) {
			// do nothing
		}	
		
        notifier.tickerText = getResources().getString(R.string.service_disconnected);
    	
        // release wakelock
        if (wl.isHeld()) wl.release();
        
        if (!UI_THREAD_IS_ACTIVE) {
            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                    new Intent(getApplicationContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

            notifier.setLatestEventInfo(getApplicationContext(), getString(R.string.app_name), notifier.tickerText, pi);        
        }
        
    	state = State.DISCONNECTED;
    }

	private static void process4140Response(final String data) {
		    	
		final String[] buf = data.substring(data.indexOf(SEVEN_REGISTERS), data.length()).split(" ");

		if (ModbusRTU.validCRC(buf, REGISTER_4140_LENGTH)) {   
			dataNotAvailable = false;
			
			setLearningFlags(new String[] {"0", "0", "0", buf[15], buf[16]});
			logItem.setTargetAfr(Integer.parseInt(buf[3] + buf[4], 16) / 10f);
			logItem.setClosedLoop(getBit(Integer.parseInt(buf[9] + buf[10], 16), 8) > 0); 
			
            if (DEBUG) Log.d(TAG, "Processed " + lastRegister + " response: " + data);
			sendRequest(REGISTER_4096_PLUS_SEVEN);            
            return;
            
		} else {
			if (DEBUG) Log.d(TAG, "bad CRC for " + lastRegister + ": " + data);
			dataNotAvailable = true;
			sendRequest();
		}
	}
	
    private static void process4096Response(final String data) {
    	
		final String[] buf = data.substring(data.indexOf(EIGHT_REGISTERS), data.length()).split(" ");

		if (ModbusRTU.validCRC(buf, REGISTER_4096_LENGTH)) {   
			dataNotAvailable = false;
    		
    		logItem.setRpm(Integer.parseInt(buf[3] + buf[4], 16));
    		logItem.setMap(Integer.parseInt(buf[5] + buf[6], 16));
    		logItem.setMat(getTemperatureValue(buf[7] + buf[8]));
    		logItem.setWat(getTemperatureValue(buf[9] + buf[10]));
    		
    		logItem.setAfr(Integer.parseInt(buf[14], 16) / 10f);
    		logItem.setReferenceAfr(Integer.parseInt(buf[13], 16) / 10f);
    		
    		logItem.setTps(Integer.parseInt(buf[17] + buf[18], 16));
    		
    		// afr logging stuff
    		if (afrAlarmLogging) {
    			final float threshold = logItem.getTargetAfr() * (afrNotEqualTargetTolerance * .01f);
    			if (Math.abs(logItem.getTargetAfr() - logItem.getAfr()) >= threshold ) {
    				afrAlarmLogItems.addLogItem(logItem);
    			}
    		}
    		
    		// log all events?
    		if (logAll) {
    			logAllItems.addLogItem(logItem);
    		}
    		
            if (DEBUG) Log.d(TAG, "Processed " + lastRegister + " response: " + data);
    		sendRequest(REGISTER_4140_PLUS_SIX);
            return;
            
		} else {
			if (DEBUG) Log.d(TAG, "bad CRC for " + lastRegister + ": " + data);
			dataNotAvailable = true;
    		sendRequest();
		}
    }
    
	private static void setLearningFlags(String[] buf) {	
		 logItem.setLearningFWait(getBit(Integer.parseInt(buf[3] + buf[4], 16), 0) > 0); 
		 logItem.setLearningFRpm(getBit(Integer.parseInt(buf[3] + buf[4], 16), 1) > 0);
		 logItem.setLearningFLoad(getBit(Integer.parseInt(buf[3] + buf[4], 16), 2) > 0); 

		 logItem.setLearningIWait(getBit(Integer.parseInt(buf[3] + buf[4], 16), 3) > 0); 
		 logItem.setLearningIRpm(getBit(Integer.parseInt(buf[3] + buf[4], 16), 4) > 0); 
		 logItem.setLearningILoad(getBit(Integer.parseInt(buf[3] + buf[4], 16), 5) > 0); 
	}
	
    private static int getBit(final int item, final int position) {   
    	return (item >> position) & 1;
    }

    private static int getTemperatureValue(String in) {
    	
    	final int temp = Integer.parseInt(in, 16);
    	
    	switch (tempUomPref) {
    		case 1:
    			return temp * 9 / 5 + 32; 
    		case 2:
    			return (int) (temp + 273.15);
    		case 3:
    			return temp * 33 / 100;
    		default:
    			return temp;
    	}
    }
    
//    private Bitmap convertToGrayscale(Drawable drawable) {
//        ColorMatrix matrix = new ColorMatrix();
//        matrix.setSaturation(0);
//
//        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
//        drawable.setColorFilter(filter);
//
//        return ((BitmapDrawable) drawable).getBitmap();
//    }
	
    private static void clearDataBuffer() {
		dataBuffer.setLength(0);
    }

    private final static StringBuffer setDataBuffer(final byte[] data, final int length) {
        for (int x = 0; x < length; x++) {
        	dataBuffer.append(String.format("%02X ", data[x]));
        }
    	return dataBuffer;
    }

    public void setTempUomPref(final int val) {
		tempUomPref = val;
    }
    
	public void setWakeLock(boolean val) {
		wakeLock = val;
		
		if (state == State.CONNECTED_BT || state == State.CONNECTED_USB) {
			if (!wakeLock && wl.isHeld()) 
				wl.release();
			else
				if (wakeLock && !wl.isHeld()) wl.acquire();
		}
	}

	public void setAfrAlarmLogging(final boolean val) {
		afrAlarmLogging = val;
	}

	public void setAfrNotEqualTargetTolerance(final float val) {
		afrNotEqualTargetTolerance = val;
	}

	public LogItems.LogItem getLogItem() {
		return logItem;
	}

	public LogItems getAfrAlarmLogItems() {
		return afrAlarmLogItems;
	}

	public boolean isDataNotAvailable() {
		return dataNotAvailable;
	}

	public State getState() {
		return state;
	}
	
	public void setLogAll(boolean val) {
		logAll = val;
	}
	
	public LogItems getLogAllItems() {
		return logAllItems;
	}
	
	public StringBuffer getMapData() {
		return mapData;
	}

	public float getAvgResponseMillis() {
		try {
			return totalTimeMillis / updatesReceived;			
		} catch (Exception ex) {
			return 0;
		}
	}
}
