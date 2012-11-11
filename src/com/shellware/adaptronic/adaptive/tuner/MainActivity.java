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

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.shellware.adaptronic.adaptive.tuner.changelog.ChangeLog;
import com.shellware.adaptronic.adaptive.tuner.gauges.GaugeNeedle;
import com.shellware.adaptronic.adaptive.tuner.preferences.AdaptivePreferences;
import com.shellware.adaptronic.adaptive.tuner.services.ConnectionService;
import com.shellware.adaptronic.adaptive.tuner.services.ConnectionService.State;
import com.shellware.adaptronic.adaptive.tuner.valueobjects.LogItems;
import com.shellware.adaptronic.adaptive.tuner.valueobjects.LogItems.LogItem;

public class MainActivity extends Activity implements ActionBar.TabListener, OnClickListener {
	
	public static final String TAG = "Adaptive";
	public static final boolean DEBUG = true;

	private static final int LONG_PAUSE = 100;

	private static final int AFR_MIN = 970;
	private static final int AFR_MAX = 1970;

	private View mActionBarView;
	private Fragment adaptiveFragment;
	private Fragment gaugesFragment;
	private Fragment fuelFragment;
	
	private static TextView txtData;
	private ListView lvDevices;
	private RelativeLayout layoutDevices;
	
	private Menu myMenu;
	private static MenuItem menuConnect;
	private static MenuItem menuShareLog;
	
	private static GridView gridData;
	private static ImageView imgStatus;
	
	private static TextView txtFuelLearn;
	private static ImageView imgIWait;
	private static ImageView imgIRpm;
	private static ImageView imgILoad;
	
	private static ImageView imgFWait;
	private static ImageView imgFRpm;
	private static ImageView imgFLoad;
	
	private ImageView imgIat;
	
	private static GaugeNeedle waterNeedle;
	private static GaugeNeedle iatNeedle;
	private static GaugeNeedle mapNeedle;
	private static GaugeNeedle afrNeedle;
	private static GaugeNeedle targetAfrNeedle;
	private static GaugeNeedle rpmNeedle;

	private static ProgressDialog progress;
	
	private static Handler refreshHandler = new Handler();

	private final IntentFilter mUsbDetachedFilter = new IntentFilter();
	private final BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();;
	
	private ArrayAdapter<String> devices;
	private static ArrayAdapter<String> dataArray;

	private static int lastRPM = 0;
	
	private static SharedPreferences prefs ;
	private static Context ctx;
	
	private static int tempUomPref = 1;
	private static boolean afrNotEqualTargetPref = false;
	private static float afrNotEqualTargetTolerance = 5f;
	private static boolean waterTempPref = false;
	private static float minimumWaterTemp = 0f;
	private static float maximumWaterTemp = 210f;
	private static String remoteMacAddr = "";
	private static String remoteName = "";
	private static boolean autoConnect = false;
	private static boolean shuttingDown = false;
	
	private static boolean afrAlarmLogging = false;
	private final static LogItems afrAlarmLogItems = new LogItems();
	
	private static boolean mapMode = false;
	private static short mapOffset = 0;
	private static StringBuffer mapData = new StringBuffer(1280);
	
	private GridView fuelGrid1;
	private GridView fuelGrid2;
	private GridView fuelGrid3;
	private GridView fuelGrid4;
	
	private ArrayAdapter<String> fuelData1;
	private ArrayAdapter<String> fuelData2;
	private ArrayAdapter<String> fuelData3;
	private ArrayAdapter<String> fuelData4;
	
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;

	private GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;
		
	private static ConnectionService connectionService;
	private ServiceConnection connectionServiceConnection;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
        ctx = this;
    	prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	
        adaptiveFragment = getFragmentManager().findFragmentById(R.id.frag_adaptive);
        gaugesFragment = getFragmentManager().findFragmentById(R.id.frag_gauges);
        fuelFragment = getFragmentManager().findFragmentById(R.id.frag_fuel);
        
        ActionBar bar = getActionBar();
        
        bar.addTab(bar.newTab().setText(R.string.tab_adaptive).setTabListener(this), false);        
        bar.addTab(bar.newTab().setText(R.string.tab_gauges).setTabListener(this), false);
        bar.addTab(bar.newTab().setText("Fuel Map").setTabListener(this), false);

        mActionBarView = getLayoutInflater().inflate(
                R.layout.action_bar_custom, null);
 
        bar.setCustomView(mActionBarView);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayShowHomeEnabled(true);

        txtData = (TextView) findViewById(R.id.txtData);
        lvDevices = (ListView) findViewById(R.id.lvDevices);
        layoutDevices = (RelativeLayout) findViewById(R.id.layoutDevices);
        imgStatus = (ImageView) findViewById(R.id.imgStatus);
        
        txtFuelLearn = (TextView) findViewById(R.id.fuellearn);
        
        imgIWait = (ImageView) findViewById(R.id.imgIWait);
		imgIRpm = (ImageView) findViewById(R.id.imgIRpm);
        imgILoad = (ImageView) findViewById(R.id.imgILoad);

        imgFWait = (ImageView) findViewById(R.id.imgFWait);
		imgFRpm = (ImageView) findViewById(R.id.imgFRpm);
        imgFLoad = (ImageView) findViewById(R.id.imgFLoad);
        
        imgIat = (ImageView) findViewById(R.id.iat);

        gridData = (GridView) findViewById(R.id.gridData);
        
        dataArray = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        dataArray.add("RPM\n ----");
        dataArray.add("MAP\n ---");
        dataArray.add("MAT\n ---\u00B0");
        dataArray.add("AFR\n --.-");
        dataArray.add("TAFR\n --.-");
        dataArray.add("WAT\n ---\u00B0");
 
        gridData.setAdapter(dataArray);   
        
        lvDevices.setOnItemClickListener(DevicesClickListener);
        
        waterNeedle = (GaugeNeedle) findViewById(R.id.waterneedle);
        iatNeedle = (GaugeNeedle) findViewById(R.id.iatneedle);
        mapNeedle = (GaugeNeedle) findViewById(R.id.mapneedle);
        afrNeedle = (GaugeNeedle) findViewById(R.id.afrneedle);
        targetAfrNeedle = (GaugeNeedle) findViewById(R.id.targetafrneedle);
        rpmNeedle = (GaugeNeedle) findViewById(R.id.rpmneedle);

        waterNeedle.setPivotPoint(.65f);
        waterNeedle.setMinValue(100);
        waterNeedle.setMaxValue(240);
        waterNeedle.setMinDegrees(-50);
        waterNeedle.setMaxDegrees(55);

        afrNeedle.setPivotPoint(.5f);
        afrNeedle.setMinValue(AFR_MIN);
        afrNeedle.setMaxValue(AFR_MAX);
        afrNeedle.setMinDegrees(-180);
        afrNeedle.setMaxDegrees(90);   
        
        targetAfrNeedle.setPivotPoint(.5f);
        targetAfrNeedle.setMinValue(AFR_MIN);
        targetAfrNeedle.setMaxValue(AFR_MAX);
        targetAfrNeedle.setMinDegrees(-180);
        targetAfrNeedle.setMaxDegrees(90);
        
        mapNeedle.setPivotPoint(.5f);
        mapNeedle.setMinValue(0);
        mapNeedle.setMaxValue(200);
        mapNeedle.setMinDegrees(-150);
        mapNeedle.setMaxDegrees(140);

        rpmNeedle.setPivotPoint(.5f);
        rpmNeedle.setMinValue(0);
        rpmNeedle.setMaxValue(9000);
        rpmNeedle.setMinDegrees(-130);
        rpmNeedle.setMaxDegrees(157); 
        
        ChangeLog cl = new ChangeLog(this);
        if (cl.firstRun()) {
            cl.getLogDialog().show();
        }
        
        fuelGrid1 = (GridView) findViewById(R.id.gridFuel1);
        fuelData1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        fuelGrid1.setAdapter(fuelData1);
        
        fuelGrid2 = (GridView) findViewById(R.id.gridFuel2);
        fuelData2 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        fuelGrid2.setAdapter(fuelData2);
        
        fuelGrid3 = (GridView) findViewById(R.id.gridFuel3);
        fuelData3 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        fuelGrid3.setAdapter(fuelData3);
        
        fuelGrid4 = (GridView) findViewById(R.id.gridFuel4);
        fuelData4 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        fuelGrid4.setAdapter(fuelData4);
        
        GridView[] fuelGrids = {fuelGrid1, fuelGrid2, fuelGrid3, fuelGrid4};
        
        // Gesture detection
        gestureDetector = new GestureDetector(new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };
        
//        fuelGrid1.setOnClickListener(this);    
        fuelGrid1.setOnTouchListener(gestureListener);
        
//        fuelGrid2.setOnClickListener(this);    
        fuelGrid2.setOnTouchListener(gestureListener);
        
//        fuelGrid3.setOnClickListener(this);    
        fuelGrid3.setOnTouchListener(gestureListener);
        
//        fuelGrid4.setOnClickListener(this);    
        fuelGrid4.setOnTouchListener(gestureListener);
		
//EVAN
		mUsbDetachedFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, mUsbDetachedFilter);
//END EVAN		
        
		connectionServiceConnection = new ServiceConnection() {
		    public void onServiceConnected(ComponentName className, IBinder service) {
		        connectionService = ((ConnectionService.ServiceBinder) service).getService();
		        if (DEBUG) Log.d(TAG, "service bound");
		    }

		    public void onServiceDisconnected(ComponentName className) {
		        connectionService = null;
		        if (DEBUG) Log.d(TAG, "service unbound");
		    }
		};
		
	    bindService(new Intent(this, 
	            ConnectionService.class), connectionServiceConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
	protected void onResume() {
    	super.onResume();
    	
    	// initialize our preferences
    	tempUomPref = Integer.parseInt(prefs.getString("prefs_uom_temp", "1"));
    	if (connectionService != null) connectionService.setTempUomPref(tempUomPref);
    	
    	afrNotEqualTargetPref = prefs.getBoolean("prefs_afrnottarget_pref", false);
    	
    	afrNotEqualTargetTolerance = prefs.getFloat("prefs_afrnottarget_tolerance_pref", 5f);
    	if (connectionService != null) connectionService.setAfrNotEqualTargetTolerance(afrNotEqualTargetTolerance);
    	
    	waterTempPref = prefs.getBoolean("prefs_watertemp_pref", false);    	
    	remoteName = prefs.getString("prefs_remote_name", "");
    	remoteMacAddr = prefs.getString("prefs_remote_mac", "");    	
    	autoConnect = prefs.getBoolean("prefs_auto_connect", false);
    	
    	// need to set gauge scale based on uom selected
    	switch (tempUomPref) {
    		case 0:
    	    	minimumWaterTemp = prefs.getFloat("prefs_min_water_temp", AdaptivePreferences.MIN_WATER_TEMP_CELCIUS);
    	    	maximumWaterTemp = prefs.getFloat("prefs_max_water_temp", AdaptivePreferences.MAX_WATER_TEMP_CELCIUS);

    	        iatNeedle.setMaxValue(100);
    	        imgIat.setImageResource(R.drawable.iatgauge_celcius);
    	        break;

    		case 1:
    	    	minimumWaterTemp = prefs.getFloat("prefs_min_water_temp", AdaptivePreferences.MIN_WATER_TEMP_FAHRENHEIT);
    	    	maximumWaterTemp = prefs.getFloat("prefs_max_water_temp", AdaptivePreferences.MAX_WATER_TEMP_FAHRENHEIT);
    	    	
    	        iatNeedle.setMaxValue(200);
    	        imgIat.setImageResource(R.drawable.iatgauge);
    	        break;
    	}

        iatNeedle.setPivotPoint(.5f);
        iatNeedle.setMinValue(0);
        iatNeedle.setMinDegrees(-180);
        iatNeedle.setMaxDegrees(90);

    	afrAlarmLogging = prefs.getBoolean("prefs_afr_alarm_logging", false); 
    	if (connectionService != null) connectionService.setAfrAlarmLogging(afrAlarmLogging);
    	
    	if (menuShareLog != null) {
    		menuShareLog.setVisible(afrAlarmLogging && !afrAlarmLogItems.getItems().isEmpty());
    	}

    	// ensure all fragments are hidden
    	FragmentTransaction ft = getFragmentManager().beginTransaction();
    	ft.hide(adaptiveFragment);
    	ft.hide(gaugesFragment);
    	ft.hide(fuelFragment);
    	ft.commit();  
    	
    	ActionBar bar = getActionBar();
    	bar.selectTab(bar.getTabAt(prefs.getInt("prefs_last_tab", 1)));    
    	
        String action = getIntent().getAction();

//        if (Intent.ACTION_MAIN.equals(action) || UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) { 
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) { 
        	if (DEBUG) Log.d(TAG, "USB Device Attached");
        	startService(new Intent(ConnectionService.ACTION_CONNECT_USB));	
        }   	

		refreshHandler.postDelayed(RefreshRunnable, LONG_PAUSE);
		startService(new Intent(ConnectionService.ACTION_UI_ACTIVE));
	}

    @Override
    protected void onPause() {
    	super.onPause();
    	
    	refreshHandler.removeCallbacks(RefreshRunnable);
    	if (!shuttingDown) startService(new Intent(ConnectionService.ACTION_UI_INACTIVE));
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();

		unregisterReceiver(mUsbReceiver);
		
		if (shuttingDown) stopService(new Intent(this, ConnectionService.class));
    	unbindService(connectionServiceConnection);
	}

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction()) &&
            		connectionService != null && connectionService.getState() == State.CONNECTED_USB) {// &&
//            		((UsbConnectedThread)connected).isUsbDevice((UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE))) {
    			if (DEBUG) Log.d(TAG, "USB Device Detached");
  
    			disconnect();
    			imgStatus.setBackgroundColor(Color.TRANSPARENT);
    		}            	
        }
    };    
    
	private final Runnable RefreshRunnable = new Runnable() {

		public void run() {
			
			// bail if no service binding available or not connected
			if (connectionService == null || 
					(connectionService != null && 
					connectionService.getState() != State.CONNECTED_BT &&
					connectionService.getState() != State.CONNECTED_USB)) {
		    	
    			if (menuConnect != null && menuConnect.getTitle().equals(getResources().getString(R.string.menu_disconnect))) {
    				menuConnect.setTitle(R.string.menu_connect);
    			}
				
    			if (connectionService != null && connectionService.getState() == State.DISCONNECTED) {
    				if (progress != null && progress.isShowing()) {
    					progress.dismiss();
    				}
    				if (autoConnect && remoteMacAddr.length() > 0) {
			    		disconnect();
			    		connect(remoteName,  remoteMacAddr);
			    	}
    			}
    			
	    		refreshHandler.postDelayed(this, LONG_PAUSE);
	    		return;
			}
			
    		dataArray.clear();
			final LogItem item = connectionService.getLogItem();
			
			final boolean fWait = item.isLearningFWait();
			final boolean fRpm = item.isLearningFRpm();
			final boolean fLoad = item.isLearningFLoad();
			
			final boolean iWait = item.isLearningIWait();
			final boolean iRpm = item.isLearningIRpm();
			final boolean iLoad = item.isLearningILoad();
			
			final boolean closedLoop = item.isClosedLoop();
			
			final int tps = item.getTps();
			final int mat = item.getMat();
			final int wat = item.getWat();
			final int map = item.getMap();
			final int rpm = item.getRpm();
			
			final float afr = item.getAfr();
			final float targetAfr = item.getTargetAfr();
			final float referenceAfr = item.getReferenceAfr();
			
			imgFWait.setBackgroundColor(fWait ? Color.parseColor("#FFCC00") : Color.TRANSPARENT);
			imgFRpm.setBackgroundColor(fRpm ? Color.GREEN : Color.TRANSPARENT);
			imgFLoad.setBackgroundColor(fLoad ? Color.GREEN : Color.TRANSPARENT);
			
			imgIWait.setBackgroundColor(iWait ? Color.parseColor("#FFCC00") : Color.TRANSPARENT);
			imgIRpm.setBackgroundColor(iRpm ? Color.GREEN : Color.TRANSPARENT);
			imgILoad.setBackgroundColor(iLoad ? Color.GREEN : Color.TRANSPARENT);

			txtFuelLearn.setBackgroundColor(closedLoop ? Color.GREEN : Color.TRANSPARENT);

			txtData.setText(String.format("AVG: %.0f ms - TPS: %d%%", connectionService.getAvgResponseMillis(), tps));

    		iatNeedle.setValue(mat);
    		waterNeedle.setValue(wat);
    		mapNeedle.setValue(map);
    		
    		float afrVal = afr * 100;
    		float targetAfrVal = targetAfr * 100;
    		
    		if (afrVal > AFR_MAX) afrVal = AFR_MAX;
    		if (afrVal < AFR_MIN) afrVal = AFR_MIN;
    		
    		if (targetAfrVal > AFR_MAX) targetAfrVal = AFR_MAX;
    		if (targetAfrVal < AFR_MIN) targetAfrVal = AFR_MIN;

    		afrNeedle.setValue(AFR_MAX - afrVal + AFR_MIN);
    		targetAfrNeedle.setValue(AFR_MAX - targetAfrVal + AFR_MIN);
			
    		if (rpm >= 200) lastRPM = rpm;
    		dataArray.add(String.format("RPM\n%d", lastRPM));
    		rpmNeedle.setValue(lastRPM);

    		dataArray.add(String.format("MAP\n%d kPa", map));
    		dataArray.add(String.format("MAT\n%d\u00B0 %s", mat, getTemperatureSymbol()));
    		dataArray.add(String.format("AFR\n%.1f (%.1f)", afr, referenceAfr));
    		dataArray.add("TAFR\n" +  (targetAfr != 0f ? String.format("%.1f", targetAfr) : "--.-"));
    		dataArray.add(String.format("WAT\n%d\u00B0 %s", wat, getTemperatureSymbol()));

    		// alarm stuff
    		if (gridData.getChildAt(3) != null && gridData.getChildAt(5) != null) {
    			
        		// water temperature
    			gridData.getChildAt(5).setBackgroundColor(Color.TRANSPARENT);
        		if (waterTempPref) {
        			if (wat < minimumWaterTemp) gridData.getChildAt(5).setBackgroundColor(Color.BLUE);
        			if (wat > maximumWaterTemp) gridData.getChildAt(5).setBackgroundColor(Color.RED);
        		}

        		// afr vs target alarm
				gridData.getChildAt(3).setBackgroundColor(Color.TRANSPARENT);
        		if (afrNotEqualTargetPref) {
        			final float threshold = targetAfr * (afrNotEqualTargetTolerance * .01f);
        			if (Math.abs(targetAfr - afr) >= threshold ) {
        				if (afr > targetAfr) {
        					gridData.getChildAt(3).setBackgroundColor(Color.RED);
        				} else {
        					gridData.getChildAt(3).setBackgroundColor(Color.BLUE);
        				}
        			}
        		}
    		}
    		
    		if (progress != null && progress.isShowing()) {
    			progress.dismiss();
    			if (menuConnect != null) menuConnect.setTitle(R.string.menu_disconnect);
    		}
    		
    		refreshHandler.postDelayed(this, LONG_PAUSE);
        }
    };

    
    private static String getTemperatureSymbol() {
    	switch (tempUomPref) {
    		case 1:
    			return "F";
    		case 2:
    			return "K";
    		case 3:
    			return "N";
			default:
				return "C";
    	}
    }
    
    //TODO create conversion for water gauge
//    private static int getTemperatureValue(String in) {
//    	
//    	final int temp = Integer.parseInt(in, 16);
//    	
//    	switch (tempUomPref) {
//    		case 1:
//    			return temp * 9 / 5 + 32; 
//    		case 2:
//    			return (int) (temp + 273.15);
//    		case 3:
//    			return temp * 33 / 100;
//    		default:
//    			return temp;
//    	}
//    }
    
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
    	
    	if (devices.getCount() > 0) layoutDevices.setVisibility(View.VISIBLE);
    }
    
    private void connect(final String name, final String macAddr) {
    	
    	if (progress != null && progress.isShowing()) return;
    	
    	Intent service = new Intent(ConnectionService.ACTION_CONNECT_BT);
    	service.putExtra("name", name);
    	service.putExtra("addr", macAddr);
    	
    	startService(service);
    	
    	progress = ProgressDialog.show(this, "Bluetooth Connection" , "Connecting to " + name);
    	progress.setCancelable(true);
    	progress.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface arg0) {
				disconnect();
			}});    	
    }
    
    private void disconnect() {
		if (menuConnect != null) menuConnect.setTitle(R.string.menu_connect);
    	if (progress != null && progress.isShowing()) progress.dismiss();

    	imgStatus.setBackgroundColor(Color.TRANSPARENT);				

		startService(new Intent(ConnectionService.ACTION_DISCONNECT));
    }
    
    @SuppressWarnings("unused")
	private static void sleep(final int millis) {
    	try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// do nothing
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        
        myMenu = menu;
        menuShareLog = myMenu.findItem(R.id.menu_share);
    	menuConnect = myMenu.findItem(R.id.menu_connect);
    	
		menuShareLog.setVisible(afrAlarmLogging && !afrAlarmLogItems.getItems().isEmpty());  
		
        return true;
    }

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		
        // Handle item selection
        switch (item.getItemId()) {
        
	        case android.R.id.home:
	        	ChangeLog cl = new ChangeLog(this);
	        	cl.getFullLogDialog().show();
	        	return true;
	        	
	        case R.id.menu_exit:
//	        	System.exit(0);
	        	shuttingDown = true;
	    		this.finish();
	        	
	        case R.id.menu_share:
	        	shareLog();
	        	return true;

	        case R.id.menu_connect:
	        	// bail if no bluetooth adapter
        		if (bt == null) return false;

        		if (item.getTitle().toString().equalsIgnoreCase(getResources().getString(R.string.menu_connect))) {
	        		showDevices();
	        	} else {
	        		disconnect();
	        		Editor edit = prefs.edit();
	        		edit.putString("prefs_remote_name", "");
	        		edit.putString("prefs_remote_mac", "");
	        		edit.commit();
	        	}
	            return false;
	            
	        case R.id.menu_prefs:
                startActivity(new Intent(this, AdaptivePreferences.class));
                return true;
                
        	default:
                return super.onOptionsItemSelected(item);
        }
    } 
	
	private void shareLog() {
		
		synchronized(this) {
			
			// if we're logging save the log file
			if (afrAlarmLogging) {		
				try {
					final String filename = new SimpleDateFormat("yyyyMMdd_HHmmss'.csv'").format(new Date());

					File sdcard = Environment.getExternalStorageDirectory();
					File dir = new File (sdcard.getAbsolutePath() + "/AdaptiveTuner/");
					dir.mkdirs();
					
					File file = new File(dir, filename);
					FileOutputStream f = new FileOutputStream(file);
					
					// write our header
					f.write("timestamp, rpm, map, closedloop, targetafr, afr, refafr, tps, wat, mat\n".getBytes());
					
					ArrayList<LogItem> items = connectionService.getAfrAlarmLogItems().getItems();
					Iterator<LogItem> iterator = items.iterator();
					
					while (iterator.hasNext()) {
						final LogItem item = (LogItem) iterator.next();
						f.write(item.getLogBytes());
					}
					
					connectionService.getAfrAlarmLogItems().getItems().clear();
					
					f.flush();
					f.close();
					
					menuShareLog.setVisible(false);
					
					Toast.makeText(getApplicationContext(), String.format("Log saved as %s%s%s", sdcard.getAbsolutePath(), "/AdaptiveTuner/", filename), Toast.LENGTH_LONG).show();
					if (DEBUG) Log.d(TAG, String.format("Log saved as %s%s%s", sdcard.getAbsolutePath(), "/AdaptiveTuner/", filename));

					Intent share = new Intent(Intent.ACTION_SEND);
					share.setType("text/plain");
					share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.getPath()));
					startActivity(Intent.createChooser(share, getText(R.string.share_log)));

				} catch (Exception e) {
					Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}
	}


	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		
		ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
		
		switch (tab.getPosition()) {
			case 0:
				ft.show(adaptiveFragment);
				break;
			case 1:
				ft.show(gaugesFragment);
				break;  
			case 2:
				ft.show(fuelFragment);
		}
		
	}
	
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {

		ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);

		switch (tab.getPosition()) {
			case 0:
				ft.hide(adaptiveFragment);
				break;
			case 1:
				ft.hide(gaugesFragment);
				break;  
			case 2:
				ft.hide(fuelFragment);
		}
	}
	
	
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		
		ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
		
		switch (tab.getPosition()) {
			case 0:
				ft.show(adaptiveFragment);
				break;
			case 1:
				ft.show(gaugesFragment);
				break;  
			case 2:				
//				if (connected != null && connected.isAlive()) {
//			    	progress = ProgressDialog.show(ctx, "Fuel Map" , "Reading map values 0/512...");
//
//			    	mapOffset = 0;
//			    	mapMode = true;
//			    	sendRequest(mapOffset);
//				}
				
				fuelData1.clear();
				fuelData2.clear();
				fuelData3.clear();
				fuelData4.clear();

				for (int x = 0; x < 128; x++) {
					fuelData1.add(String.format("%d", x));
					fuelData2.add(String.format("%d", x + 128));
					fuelData3.add(String.format("%d", x + 256));
					fuelData4.add(String.format("%d", x + 384));
				}	  
			
				ft.show(fuelFragment);
				break;
		}
		
		// we don't want to overwrite our pref if we're in onCreate
		if (lvDevices != null) {
			Editor edit = prefs.edit();
			edit.putInt("prefs_last_tab", tab.getPosition());
			edit.commit();
		}
	}

	@Override
	public void onBackPressed() {

		if (layoutDevices.getVisibility() == View.VISIBLE) { 
			layoutDevices.setVisibility(View.INVISIBLE);
		} else {
			super.onBackPressed();
		}
	}

	public void onClick(View arg0) {
//        Filter f = (Filter) v.getTag();
//        FilterFullscreenActivity.show(this, input, f);
	}
	
    class MyGestureDetector extends SimpleOnGestureListener {

        final private ViewFlipper vf = (ViewFlipper) findViewById(R.id.gridFlipper);
        
        final private Animation animFlipInForeward = AnimationUtils.loadAnimation(ctx, R.anim.flipin);
        final private Animation animFlipOutForeward = AnimationUtils.loadAnimation(ctx, R.anim.flipout);
        final private Animation animFlipInBackward = AnimationUtils.loadAnimation(ctx, R.anim.flipin_reverse);
        final private Animation animFlipOutBackward = AnimationUtils.loadAnimation(ctx, R.anim.flipout_reverse);
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) return false;
                
                // right to left swipe
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
//                    Toast.makeText(ctx, "Left Swipe", Toast.LENGTH_SHORT).show();

//                    vf.setAnimation(AnimationUtils.loadAnimation(ctx, android.R.anim.slide_out_right));
                    vf.setInAnimation(animFlipInForeward);
                    vf.setOutAnimation(animFlipOutForeward);
                    
                    vf.showNext();
                    
                }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
//                    Toast.makeText(ctx, "Right Swipe", Toast.LENGTH_SHORT).show();
                    
//                    vf.setAnimation(AnimationUtils.loadAnimation(ctx, android.R.anim.slide_in_left));
                    vf.setInAnimation(animFlipInBackward);
                    vf.setOutAnimation(animFlipOutBackward);
                    
                    vf.showPrevious();                    
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }

    }
}
