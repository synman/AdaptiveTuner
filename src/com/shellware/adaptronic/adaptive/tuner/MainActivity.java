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
import java.util.Locale;
import java.util.Set;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
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
import android.graphics.PixelFormat;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.shellware.adaptronic.adaptive.tuner.changelog.ChangeLog;
import com.shellware.adaptronic.adaptive.tuner.gauges.GaugeNeedle;
import com.shellware.adaptronic.adaptive.tuner.gauges.GaugeSlider;
import com.shellware.adaptronic.adaptive.tuner.preferences.AdaptivePreferences;
import com.shellware.adaptronic.adaptive.tuner.receivers.BatteryStatusReceiver;
import com.shellware.adaptronic.adaptive.tuner.services.ConnectionService;
import com.shellware.adaptronic.adaptive.tuner.services.ConnectionService.State;
import com.shellware.adaptronic.adaptive.tuner.valueobjects.LogItems.LogItem;

public class MainActivity extends Activity implements ActionBar.TabListener, OnClickListener {
	
	public static final String TAG = "Adaptive";
	public static final boolean DEBUG = false;

	private static final int LONG_PAUSE = 250;

//	private static final int AFR_MIN = 970;
//	private static final int AFR_MAX = 1970;
	private static final int AFR_MIN = 800;
	private static final int AFR_MAX = 1800;

	
	private static final float VE_DIVISOR = 128f;
	private static final float MS_DIVISOR = 1500f;
	
	private static final String LOG_HEADER = "timestamp, rpm, map, closedloop, targetafr, afr, refafr, tps, wat, mat, knock, volts\n";
	
	private Fragment adaptiveFragment;
	private Fragment gaugesFragment;
	private Fragment fuelFragment;
	
	private final Fragment[] frags = { adaptiveFragment, gaugesFragment, fuelFragment };
	
	private TextView txtData;
	private ListView lvDevices;
	private RelativeLayout layoutDevices;
	
	private Menu myMenu;
	private MenuItem menuConnect;
	private MenuItem menuUsbConnect;
	private MenuItem menuShareLog;
	
	private GridView gridData;
	private ImageView imgStatus;
	
	private TextView txtFuelLearn;
	private ImageView imgIWait;
	private ImageView imgIRpm;
	private ImageView imgILoad;
	
	private ImageView imgFWait;
	private ImageView imgFRpm;
	private ImageView imgFLoad;
	
	private ImageView imgIat;
	
	private TextView fuelTableAfr;
	private ImageView crossX;
	private ImageView crossY;
	
	private GaugeNeedle waterNeedle;
	private GaugeNeedle iatNeedle;
	private GaugeNeedle mapNeedle;
	private GaugeNeedle afrNeedle;
	private GaugeNeedle targetAfrNeedle;
	private GaugeNeedle rpmNeedle;
	
	private GaugeSlider tpsSlider;
	
	private ImageView afrGaugeAlarm;
	private ImageView waterGaugeAlarm;

	private ProgressDialog progress;
	
	private Handler refreshHandler;

	private final IntentFilter usbDetachedFilter = new IntentFilter();
	private final BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();;
	
	private ArrayAdapter<String> devices;
	private ArrayAdapter<String> dataArray;

	private static int lastMAP = 0;
	private static int lastRPM = 0;
	private static int lastTPS = 0;
	
	private static boolean lastClosedLoop = false;
	
	private SharedPreferences prefs ;
	private Context ctx;
	
	private static int screenWidth = 0;
//	private static int screenHeight = 0;
	
	private static int tempUomPref = 1;
	
	private static boolean wakeLock = true;
	private static boolean afrNotEqualTargetPref = false;
	private static float afrNotEqualTargetTolerance = 5f;
	private static boolean waterTempPref = false;
	private static float minimumWaterTemp = 0f;
	private static float maximumWaterTemp = 210f;
	private static String remoteMacAddr = "";
	private static String remoteName = "";
	private static boolean autoConnect = false;
	private static boolean shuttingDown = false;
	
	private static boolean mapMode = false;
	public static boolean mapReady = false;
	
	private static boolean logAll = false;
	private static boolean afrAlarmLogging = false;
	
	private GridView fuelGridHeaderTop;
	private GridView fuelGrid;

	private RadioButton radioMapOne;
	private RadioButton radioMapTwo;
	
	private ArrayAdapter<String> fuelDataTop;
	private ArrayAdapter<String> fuelData;

//	private static final int SWIPE_MIN_DISTANCE = 120;
//	private static final int SWIPE_MAX_OFF_PATH = 250;
//	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
//
//	private GestureDetector gestureDetector;
//	View.OnTouchListener gestureListener;
		
	private ConnectionService connectionService;
	private ServiceConnection connectionServiceConnection;
	
	private BatteryStatusReceiver batteryStatusReceiver;
	
	@Override
	public void onAttachedToWindow() {
	    super.onAttachedToWindow();
	    Window window = getWindow();
	    window.setFormat(PixelFormat.RGBA_8888);
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
        ctx = this;
    	prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	
    	// Instantiate all of our fragments
        adaptiveFragment = getFragmentManager().findFragmentById(R.id.frag_adaptive);
        gaugesFragment = getFragmentManager().findFragmentById(R.id.frag_gauges);
        fuelFragment = getFragmentManager().findFragmentById(R.id.frag_fuel);
        
        frags[0] = adaptiveFragment;
        frags[1] = gaugesFragment;
        frags[2] = fuelFragment;
                
        final ActionBar bar = getActionBar();
        
        // ActionBar settings common across orientations
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_USE_LOGO);
        // get current screen orientation
        final int o = getResources().getConfiguration().orientation;
         
        // build our navigation model based on orientation
        if (o != Surface.ROTATION_0 && o != Surface.ROTATION_180) {
        	// portrait
            bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            
            // our navigation list items
            final String[] actions = new String[] { getResources().getString(R.string.tab_adaptive), 
            										getResources().getString(R.string.tab_gauges), 
            										getResources().getString(R.string.tab_fuel_map) };
            // define our array adapter
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx, android.R.layout.simple_spinner_dropdown_item, actions);
            
            // define our listener 
            OnNavigationListener navigationListener = new OnNavigationListener() {
                public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                	FragmentTransaction ft = getFragmentManager().beginTransaction();
                	
                	ft.hide(adaptiveFragment);
                	ft.hide(gaugesFragment);
                	ft.hide(fuelFragment);
                	
                	//kludge to populate fuel map
                	if (itemPosition == 2) getFuelMaps();
               
                	ft.show(frags[itemPosition]);
                	ft.commit();

        			Editor edit = prefs.edit();
        			edit.putInt("prefs_last_tab",itemPosition);
        			edit.commit();

                	return true;
                }
            };
            
            // bind our  adapter and listener
           bar.setListNavigationCallbacks(adapter, navigationListener);
        } else {
        	// landscape
            bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            bar.addTab(bar.newTab().setText(R.string.tab_adaptive).setTabListener(this), false);        
            bar.addTab(bar.newTab().setText(R.string.tab_gauges).setTabListener(this), false);
            bar.addTab(bar.newTab().setText(R.string.tab_fuel_map).setTabListener(this), false);
        }

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
        
        tpsSlider = (GaugeSlider) findViewById(R.id.tpsslider);

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
        
        tpsSlider.setMinValue(0);
        tpsSlider.setMaxValue(100);
        tpsSlider.setSuffix("%");
        
        afrGaugeAlarm = (ImageView) findViewById(R.id.afrmeteralarm);
        waterGaugeAlarm = (ImageView) findViewById(R.id.watermeteralarm);
        
        ChangeLog cl = new ChangeLog(this);
        if (cl.firstRun()) {
            cl.getLogDialog().show();
        }
        
        fuelGrid = (GridView) findViewById(R.id.gridFuel);
        fuelData = new ArrayAdapter<String>(this, R.layout.tiny_list_item);
        fuelGrid.setAdapter(fuelData);
        
        fuelGridHeaderTop = (GridView) findViewById(R.id.gridFuelHeaderTop);
        fuelDataTop = new ArrayAdapter<String>(this, R.layout.tiny_list_item_bold);
        fuelGridHeaderTop.setAdapter(fuelDataTop);
        
        crossX = (ImageView) findViewById(R.id.crossX);
        crossY = (ImageView) findViewById(R.id.crossY);
        
        fuelTableAfr = (TextView) findViewById(R.id.fuelTabAfr);
        
        radioMapOne = (RadioButton) findViewById(R.id.radioMapOne);
        radioMapTwo = (RadioButton) findViewById(R.id.radioMapTwo);
        
        radioMapOne.setOnClickListener(this);
        radioMapTwo.setOnClickListener(this);
        
        // default fuel table header
//        onxClick(radioMapOne);
        
        usbDetachedFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, usbDetachedFilter);
        
	    batteryStatusReceiver = new BatteryStatusReceiver();
	    registerReceiver(batteryStatusReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

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
		
	    bindService(new Intent(this, ConnectionService.class), connectionServiceConnection, Context.BIND_AUTO_CREATE);	    
    }
    
    @Override
	protected void onResume() {
    	super.onResume();
    	
    	// set screen dimensions
    	screenWidth = getWindowManager().getDefaultDisplay().getWidth();
//    	screenHeight = getWindowManager().getDefaultDisplay().getHeight();

    	// initialize our preferences
    	tempUomPref = Integer.parseInt(prefs.getString("prefs_uom_temp", "1"));
    	if (connectionService != null) connectionService.setTempUomPref(tempUomPref);
    	
    	wakeLock = prefs.getBoolean("prefs_wake_lock", true);
    	if (connectionService != null) connectionService.setWakeLock(wakeLock);
    	
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
        
        logAll = prefs.getBoolean("prefs_log_all", false);
        if (connectionService != null) connectionService.setLogAll(logAll);

    	afrAlarmLogging = prefs.getBoolean("prefs_afr_alarm_logging", false); 
    	if (connectionService != null) connectionService.setAfrAlarmLogging(afrAlarmLogging);
    	
    	if (menuShareLog != null && connectionService != null) {
    		menuShareLog.setVisible((afrAlarmLogging && !connectionService.getAfrAlarmLogItems().getItems().isEmpty()) ||
    								 (logAll && !connectionService.getLogAllItems().getItems().isEmpty()));
    	}
    	
    	// ensure all fragments are hidden
    	FragmentTransaction ft = getFragmentManager().beginTransaction();
    	ft.hide(adaptiveFragment);
    	ft.hide(gaugesFragment);
    	ft.hide(fuelFragment);

    	final ActionBar bar = getActionBar();	
        final int o = getResources().getConfiguration().orientation;   
        final int savedTabIndex = prefs.getInt("prefs_last_tab", 1);
        
        // set default navigation item based on orientation
        if (o != Surface.ROTATION_0 && o != Surface.ROTATION_180) {
        	// portrait
            bar.setSelectedNavigationItem(savedTabIndex);
        	if (savedTabIndex == 2) getFuelMaps();
        	ft.show(frags[savedTabIndex]);
        	ft.commit();  
        } else {
        	// landscape
        	ft.commit(); // must commit before selecting tab  
        	bar.selectTab(bar.getTabAt(savedTabIndex));            	
        }

        // check our intent to see if we received a USB attachment
        String action = getIntent().getAction();

        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) { 
        	if (DEBUG) Log.d(TAG, "USB Device Attached");
        	startService(new Intent(ConnectionService.ACTION_CONNECT_USB));	
        }   	

        // refresh the screen (atleast) once
        refreshHandler = new Handler();
		refreshHandler.postDelayed(RefreshRunnable, LONG_PAUSE);
		
		startService(new Intent(ConnectionService.ACTION_UI_ACTIVE));
	}

    @Override
    protected void onPause() {
    	super.onPause();

    	refreshHandler.removeCallbacks(RefreshRunnable);
    	refreshHandler = null;
    	
    	if (!shuttingDown) startService(new Intent(ConnectionService.ACTION_UI_INACTIVE));
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();

		unregisterReceiver(usbReceiver);
		unregisterReceiver(batteryStatusReceiver);
		
		if (shuttingDown) stopService(new Intent(this, ConnectionService.class));
    	unbindService(connectionServiceConnection);
	}

	private final Runnable RefreshRunnable = new Runnable() {

		public void run() {

			// bail if no service binding available or not connected
			if (connectionService == null || 
					(connectionService.getState() != State.CONNECTED_BT &&
					connectionService.getState() != State.CONNECTED_USB)) {
		    	
    			if (menuConnect != null && menuConnect.getTitle().equals(getResources().getString(R.string.menu_disconnect))) {
    				menuConnect.setTitle(R.string.menu_connect);
    			}
				
    			if (menuUsbConnect != null && menuUsbConnect.getTitle().equals(getResources().getString(R.string.menu_disconnect))) {
    				menuUsbConnect.setTitle(R.string.menu_usb_connect);
    			}

    			if (connectionService != null && connectionService.getState() == State.DISCONNECTED) {
    				
        			imgStatus.setBackgroundColor(Color.TRANSPARENT);

        			if (progress != null && progress.isShowing()) {
    					progress.dismiss();
    				}
    				if (autoConnect && remoteMacAddr.length() > 0) {
			    		disconnect();
			    		connect(remoteName,  remoteMacAddr);
			    		
			    		// short circuit repetitive reconnect attempts
			    		autoConnect = false;
			    	}
    			}
    			
    			//TODO: why am I respawning refreshHandler if the connection is dead?
        		if (refreshHandler != null) refreshHandler.postDelayed(this, LONG_PAUSE);
	    		return;
			}

			// toggle our status image based on data reception
			imgStatus.setBackgroundColor(connectionService.isDataNotAvailable() ? Color.RED : Color.GREEN);
			
			// show the log share menu option if logging is enabled
			if (connectionService != null && menuShareLog != null)
	    		menuShareLog.setVisible((afrAlarmLogging && !connectionService.getAfrAlarmLogItems().getItems().isEmpty()) ||
						 (logAll && !connectionService.getLogAllItems().getItems().isEmpty()));				
			
	    	// show our fuel table fragment if an updated
	    	// map is available (because we asked it for)
	    	if (mapMode) {
	    		if (mapReady) {
	    			// ensure the first map isn't selected if it is not selectable
	    			if (connectionService != null) {
	    				if (connectionService.getTuningMode() == 7) {
	    					onClick(radioMapTwo);
	    				} else {
	    					onClick(radioMapOne);
	    				}
	    			}
	    			
			    	mapMode = false;
	        		mapReady = false;
	    		}
	    		if (refreshHandler != null) refreshHandler.postDelayed(this, LONG_PAUSE);
        		return;
	    	}
	    	
			// populate all data elements
    		dataArray.clear();
			final LogItem item = connectionService.getLogItem();
			
			final boolean fWait = item.isLearningFWait();
			final boolean fRpm = item.isLearningFRpm();
			final boolean fLoad = item.isLearningFLoad();
			
			final boolean iWait = item.isLearningIWait();
			final boolean iRpm = item.isLearningIRpm();
			final boolean iLoad = item.isLearningILoad();
			
			final boolean closedLoop = item.isClosedLoop();
			
			final int tps = item.getTps(); lastTPS = tps;
			final int mat = item.getMat();
			final int wat = item.getWat();
			final int map = item.getMap(); lastMAP = map;
			
			final int rpm = item.getRpm();
			
			final float afr = item.getAfr();
			final float targetAfr = item.getTargetAfr();
			final float referenceAfr = item.getReferenceAfr();
			
			if (adaptiveFragment.isVisible()) {
				imgFWait.setBackgroundColor(fWait ? Color.parseColor("#FFCC00") : Color.TRANSPARENT);
				imgFRpm.setBackgroundColor(fRpm ? Color.GREEN : Color.TRANSPARENT);
				imgFLoad.setBackgroundColor(fLoad ? Color.GREEN : Color.TRANSPARENT);
				
				imgIWait.setBackgroundColor(iWait ? Color.parseColor("#FFCC00") : Color.TRANSPARENT);
				imgIRpm.setBackgroundColor(iRpm ? Color.GREEN : Color.TRANSPARENT);
				imgILoad.setBackgroundColor(iLoad ? Color.GREEN : Color.TRANSPARENT);
	
				txtFuelLearn.setBackgroundColor(closedLoop ? Color.GREEN : Color.TRANSPARENT);
			}
			
			txtData.setText(String.format("AVG: %.0f ms -- BAT: %.1f V", connectionService.getAvgResponseMillis(), item.getVolts()));

			tpsSlider.setValue(tps);
			
			iatNeedle.setValue(mat); 
			waterNeedle.setValue(convertWat(wat));
			mapNeedle.setValue(map); 
    		
			float afrVal = afr * 100;
    		float targetAfrVal = targetAfr * 100;
    		
    		if (afrVal > AFR_MAX) afrVal = AFR_MAX;
    		if (afrVal < AFR_MIN) afrVal = AFR_MIN;
    		
    		if (targetAfrVal > AFR_MAX) targetAfrVal = AFR_MAX;
    		if (targetAfrVal < AFR_MIN) targetAfrVal = AFR_MIN;

//			afrNeedle.setValue(AFR_MAX - afrVal + AFR_MIN);
			afrNeedle.setValue(afrVal);

			if (closedLoop != lastClosedLoop) {
				targetAfrNeedle.setImageResource(closedLoop ? R.drawable.needle_middle_green : R.drawable.needle_middle_yellow);
				lastClosedLoop = closedLoop;
			}
			
//			targetAfrNeedle.setValue(AFR_MAX - targetAfrVal + AFR_MIN);
			targetAfrNeedle.setValue(targetAfrVal);
    		
    		if (rpm >= 200) lastRPM = rpm;
    		dataArray.add(String.format("RPM\n%d", lastRPM));
    		
			rpmNeedle.setValue(lastRPM); 

    		dataArray.add(String.format("MAP\n%d kPa", map));
    		dataArray.add(String.format("MAT\n%d\u00B0 %s", mat, getTemperatureSymbol()));
    		dataArray.add(String.format("AFR\n%.1f (%.1f)", afr, referenceAfr));
    		dataArray.add("TAFR\n" +  (targetAfr != 0f ? String.format("%.1f", targetAfr) : "--.-"));
    		dataArray.add(String.format("WAT\n%d\u00B0 %s", wat, getTemperatureSymbol()));

    		fuelTableAfr.setText(String.format("AFR: %.1f (%.1f)", afr, referenceAfr));
    		
    		// alarm stuff

    		// water temperature
			if (gridData.getChildAt(3) != null && gridData.getChildAt(5) != null) gridData.getChildAt(5).setBackgroundColor(Color.TRANSPARENT);
			waterGaugeAlarm.setBackgroundColor(Color.TRANSPARENT);
			
    		if (waterTempPref) {
    			if (wat < minimumWaterTemp) {
    				if (gridData.getChildAt(3) != null && gridData.getChildAt(5) != null) gridData.getChildAt(5).setBackgroundColor(Color.BLUE);
    				waterGaugeAlarm.setBackgroundColor(Color.BLUE);
    			}
    			if (wat > maximumWaterTemp) {
    				if (gridData.getChildAt(3) != null && gridData.getChildAt(5) != null) gridData.getChildAt(5).setBackgroundColor(Color.RED);
    				waterGaugeAlarm.setBackgroundColor(Color.RED);
    			}
    		}

    		// afr vs target alarm
    		if (gridData.getChildAt(3) != null) gridData.getChildAt(3).setBackgroundColor(Color.TRANSPARENT);
			fuelTableAfr.setBackgroundColor(Color.TRANSPARENT);
			afrGaugeAlarm.setBackgroundColor(Color.TRANSPARENT);
			
    		if (afrNotEqualTargetPref) {
    			final float threshold = targetAfr * (afrNotEqualTargetTolerance * .01f);
    			if (Math.abs(targetAfr - afr) >= threshold ) {
    				final int color =  afr > targetAfr ? Color.RED : Color.BLUE;
    				if (gridData.getChildAt(3) != null) gridData.getChildAt(3).setBackgroundColor(color);
    				fuelTableAfr.setBackgroundColor(color);
    				afrGaugeAlarm.setBackgroundColor(color);
    			}
    		}
    		
    		// fuel map crosshairs
    		if (fuelFragment.isVisible()) setCurrentCell();

    		// dismiss the progress bar if it is visible
    		if (progress != null && progress.isShowing()) {
    			progress.dismiss();
    			if (menuConnect != null) menuConnect.setTitle(R.string.menu_disconnect);
    		}
    		
    		if (refreshHandler != null) refreshHandler.postDelayed(this, LONG_PAUSE);
        }
    };
    
    private void setCurrentCell() {
    	try {	
    		// x axis
			final int TUNING_MODE = connectionService.getTuningMode();
			final int cellWidth = fuelGrid.getChildAt(0).getWidth();

			float multiplier = 0;
			float refValue = 0;
			float offsetX = 0;
			
    		// are we in MAP or TPS mode?
			if ((radioMapOne.isChecked() && TUNING_MODE != 7 && TUNING_MODE != 10) ||
					(radioMapTwo.isChecked() && TUNING_MODE != 0 && TUNING_MODE != 9 && TUNING_MODE != 13)) {
		    	// map mode 	
		    	multiplier = (screenWidth - cellWidth)  / connectionService.getMaxMapValue();
		    	refValue = lastMAP;
			} else {
				multiplier = (screenWidth - cellWidth) / 100;
		    	refValue = lastTPS;
			}

	    	offsetX = refValue * multiplier + cellWidth;
	    	crossX.setX(offsetX);

	    	// y axis   	
	    	final TextView tvLow = (TextView) fuelGrid.getChildAt(0);
	       	final TextView tvHigh = (TextView) fuelGrid.getChildAt(fuelGrid.getChildCount() - 17);
	    	
	    	final int lowRPM = Integer.parseInt(tvLow.getText().toString());
	    	final int highRPM = Integer.parseInt(tvHigh.getText().toString());
	    	
	    	if (lastRPM >= lowRPM && lastRPM <= highRPM && lastRPM > 0) {
	    		final float distance = lastRPM - lowRPM;
	    		final int row = (int) distance / connectionService.getRpmStepSize();
	    		
	    		TextView tvLast = (TextView) fuelGrid.getChildAt(row * 17);

	    		crossY.setY(fuelGrid.getY() + tvLast.getY() + tvLast.getHeight() / 2);
    		} else {
				crossY.setY(lastRPM > highRPM ? fuelGrid.getBottom() - 1 : fuelGrid.getTop() - 1);
	    	}
    	} catch (Exception ex) {
    		Log.d(TAG, "Unknown exception thrown in setCurrentCell");
    	}
    }
    
	private void populateFuelTable(final StringBuffer data, final boolean isVE) {
		
		// bail if data is null or empty
		if (data == null || data.length() == 0) return;
		
		try {
			final int rpmStepSize = connectionService.getRpmStepSize();
			final String[] map = data.toString().trim().split(" ");
			short cnt = 0;
	
			fuelData.clear();
	
			for (int x = 0; x < 32; x++) {
				fuelData.add(String.format("%d", x * rpmStepSize));
				for (int y = 0; y < 16; y++) {
					final double val = Double.parseDouble(String.format(Locale.US, "%.2f", 
										Integer.parseInt(map[cnt] + map[cnt+1], 16) / 
										(isVE ? VE_DIVISOR : MS_DIVISOR)));
	
					fuelData.add(String.format("%.2f", val));
	
					if (DEBUG) Log.d(TAG, String.format("%d:%d = %.2f", x, y, val));
					cnt = (short) (cnt + 2);
				}
			}			
		} catch (Exception ex) {
			// do nothing
		}
	}
	
    private String getTemperatureSymbol() {
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
    
    // necessary because currently the WAT gauge is scaled for F
    private int convertWat(final int temp) {	
    	switch (tempUomPref) {
    		case 1:
    			return temp;
    		default:
    			return (int) (temp * 1.8 + 32);
    	}
    }
    
    BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction()) &&
            		connectionService != null && connectionService.getState() == State.CONNECTED_USB) {
    			if (DEBUG) Log.d(TAG, "USB Device Detached");
  
    			disconnect();
    			imgStatus.setBackgroundColor(Color.TRANSPARENT);
    		}            	
        }
    };    
    
    private OnItemClickListener DevicesClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

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
		if (menuUsbConnect != null) menuUsbConnect.setTitle(R.string.menu_usb_connect);
		
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
    	menuUsbConnect = myMenu.findItem(R.id.menu_usb_connect);
    	
    	// show share button if logging
    	if (connectionService != null)
    		menuShareLog.setVisible((afrAlarmLogging && !connectionService.getAfrAlarmLogItems().getItems().isEmpty()) ||
					 (logAll && !connectionService.getLogAllItems().getItems().isEmpty()));
		
    	if (connectionService != null && connectionService.getState() == State.DISCONNECTED) {
    		menuConnect.setTitle(R.string.menu_connect);
    		menuUsbConnect.setTitle(R.string.menu_usb_connect);
    	} else {
    		if (connectionService != null && connectionService.getState() == State.CONNECTED_BT) {
    			menuConnect.setTitle(R.string.menu_disconnect);
    		} else {
        		if (connectionService != null && connectionService.getState() == State.CONNECTED_USB) {
        			menuConnect.setTitle(R.string.menu_disconnect);
        		}
    		}
    	}
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
	        	shuttingDown = true;
	    		this.finish();
	    		return true;
	        	
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
	            return true;
	            
	        case R.id.menu_usb_connect:
	        	if (DEBUG) Log.d(TAG, "USB Connect Selected");
	        	startService(new Intent(ConnectionService.ACTION_CONNECT_USB));	
	        	return true;
	        	
	        case R.id.menu_prefs:
                startActivity(new Intent(this, AdaptivePreferences.class));
                return true;
                
	        case R.id.menu_save_map:	        	
	        	final File sdcard = Environment.getExternalStorageDirectory();
				final File dir = new File (sdcard.getAbsolutePath() + "/AdaptiveTuner/");
				dir.mkdirs();
				
				final String filename = new SimpleDateFormat("yyyyMMdd_HHmmss'.ecu'", Locale.US).format(new Date());
				
	        	final Intent sm = new Intent(ConnectionService.ACTION_SAVE_MAP);
	        	sm.putExtra("map_filename", dir.getAbsolutePath() + filename);
	        	
	        	return true;
                
	        case R.id.menu_info:
	        	final String info = String.format(Locale.US, "Map 1 VE: %d Map 2 VE: %d Mode: %d RPM Step: %d Max MAP: %d",
						        								connectionService.isFuelMapOneVE() ? 1 : 0,
						        								connectionService.isFuelMapTwoVE() ? 1 : 0,
						        								connectionService.getTuningMode(),
						        								connectionService.getRpmStepSize(),
						        								connectionService.getMaxMapValue());

	        	Toast.makeText(getApplicationContext(), info, Toast.LENGTH_LONG).show();
	        	return true;

        	default:
                return super.onOptionsItemSelected(item);
        }
    } 
	
	private void shareLog() {
		
		// if we're logging save the log file
		if (afrAlarmLogging) {		
			try {
				final String filename = new SimpleDateFormat("yyyyMMdd_HHmmss'.afr.csv'", Locale.US).format(new Date());

				File sdcard = Environment.getExternalStorageDirectory();
				File dir = new File (sdcard.getAbsolutePath() + "/AdaptiveTuner/");
				dir.mkdirs();
				
				File file = new File(dir, filename);
				FileOutputStream f = new FileOutputStream(file);
				
				// write our header
				f.write(LOG_HEADER.getBytes());
				
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
				
				final String logLocation = String.format(getResources().getString(R.string.share_log_message), 
														 sdcard.getAbsolutePath(), "/AdaptiveTuner/", filename);
				
				Toast.makeText(getApplicationContext(), logLocation, Toast.LENGTH_LONG).show();
				if (DEBUG) Log.d(TAG, logLocation);

				Intent share = new Intent(Intent.ACTION_SEND);
				share.setType("text/plain");
				share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.getPath()));
				startActivity(Intent.createChooser(share, getText(R.string.share_afr_log_heading)));

			} catch (Exception e) {
				Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
		
		//TODO: normalize log share code
		if (logAll) {		
			try {
				final String filename = new SimpleDateFormat("yyyyMMdd_HHmmss'.all.csv'", Locale.US).format(new Date());

				File sdcard = Environment.getExternalStorageDirectory();
				File dir = new File (sdcard.getAbsolutePath() + "/AdaptiveTuner/");
				dir.mkdirs();
				
				File file = new File(dir, filename);
				FileOutputStream f = new FileOutputStream(file);
				
				// write our header
				f.write(LOG_HEADER.getBytes());
				
				ArrayList<LogItem> items = connectionService.getLogAllItems().getItems();
				Iterator<LogItem> iterator = items.iterator();
				
				while (iterator.hasNext()) {
					final LogItem item = (LogItem) iterator.next();
					f.write(item.getLogBytes());
				}
				
				connectionService.getLogAllItems().getItems().clear();
				
				f.flush();
				f.close();
				
				menuShareLog.setVisible(false);
				
				final String logLocation = String.format(getResources().getString(R.string.share_log_message), 
														 sdcard.getAbsolutePath(), "/AdaptiveTuner/", filename);
				
				Toast.makeText(getApplicationContext(), logLocation, Toast.LENGTH_LONG).show();
				if (DEBUG) Log.d(TAG, logLocation);

				Intent share = new Intent(Intent.ACTION_SEND);
				share.setType("text/plain");
				share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.getPath()));
				startActivity(Intent.createChooser(share, getText(R.string.share_log_all_heading)));

			} catch (Exception e) {
				Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
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
				getFuelMaps();
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
	
	private void getFuelMaps() {
    	fuelData.clear();      		

    	if (ConnectionService.state != ConnectionService.State.DISCONNECTED) {
			mapMode = true;
	    	progress = ProgressDialog.show(ctx, "Fuel Map" , "Reading map values ...");
	    	progress.setCancelable(true);
	    	progress.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface arg0) {
					disconnect();
				}});
	    	
        	startService(new Intent(ConnectionService.ACTION_UPDATE_FUEL_MAP));	
		} else {
			final int rpmStepSize = connectionService != null ? connectionService.getRpmStepSize() : 500;
			
			for (int x = 0; x < 32; x++) {
				fuelData.add(String.format("%d", x * rpmStepSize));
				for (int y = 0; y < 16; y++) {
					fuelData.add("---.--");
				}
			}	
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

	public void onClick(View view) {
		
		if (view.getId() == R.id.radioMapOne || view.getId() == R.id.radioMapTwo) {

			final RadioButton radio = (RadioButton) view;
			
			final int TUNING_MODE = connectionService != null ? connectionService.getTuningMode() : 0;
			final int MAX_MAP_VALUE = connectionService != null ? connectionService.getMaxMapValue() : 200;
			final String NO_MAP_FOR_MODE = getResources().getString(R.string.no_map_for_mode_selected);
			
			// filter out the impossibles
			if (radio.getId() == R.id.radioMapOne) {
				if (TUNING_MODE == 7) {
					radio.setChecked(false);
					Toast.makeText(getApplicationContext(), String.format(NO_MAP_FOR_MODE, 1), Toast.LENGTH_LONG).show();
					return;
				}
			} else {
				if (TUNING_MODE == 0) {
					radio.setChecked(false);
					Toast.makeText(getApplicationContext(), String.format(NO_MAP_FOR_MODE, 2), Toast.LENGTH_LONG).show();
					return;
				}				
			}

			radioMapOne.setChecked(false);
			radioMapTwo.setChecked(false);
			
			radio.setChecked(true);
			
			float MULTIPLIER = 0f;
			String STRING_FORMAT = "";

			fuelDataTop.clear();
			
		      //  0 - 1=MAP, 2=unused
		      //  1 - 1=MAP (default), 2=TPS (fallback)
		      //  2 - 1=MAP (fallback), 2=TPS (default)
		      //  3 - 1=MAP, 2=TPS, fuel=max(MAP,TPS)
		      //  4 - 1=MAP, 2=TPS, fuel=avg(MAP,TPS)
		      //  5 - 1=MAP, 2=TPS, fuel=min(MAP,TPS)
		      //  6 - 1=MAP, 2=TPS, fuel=MAP+TPS (both signed)
		      //  7 - 1=unused, 2=TPS
		      //  8 - 1=MAP, 2=TPS - use MAP on closed throttle
		      //  9 - 1=MAP, 2=MAP, digital input to select #2
		      // 10 - 1=TPS, 2=TPS, digital input to select #2
		      // 11 - 1=MAP, 2=TPS, digital input to select #2
		      // 12 - 1=MAP, 2=TPS, fuel=MAP*TPS
		      // 13 - 1=MAP, 2=MAP, Pri = 1, Sec = 1x2, Ign = 1
			
			switch (radio.getId()) {
				case R.id.radioMapOne:
					if (TUNING_MODE != 7 && TUNING_MODE != 10) {
						MULTIPLIER = MAX_MAP_VALUE / 15f;
						STRING_FORMAT = "%d kPA";
				        fuelDataTop.add("MAP/\nRPM");						
					} else {
						MULTIPLIER = 6.6667f;
						STRING_FORMAT = "%d%%";
				        fuelDataTop.add("TPS/\nRPM");						
					}
		    		populateFuelTable(connectionService.getMapOneData(), connectionService.isFuelMapOneVE());
					break;
				case R.id.radioMapTwo:
					if (TUNING_MODE != 0 && TUNING_MODE != 9 && TUNING_MODE != 13) {
						MULTIPLIER = 6.6667f;
						STRING_FORMAT = "%d%%";
				        fuelDataTop.add("TPS/\nRPM");						
					} else {
						MULTIPLIER = MAX_MAP_VALUE / 15f;
						STRING_FORMAT = "%d kPA";
				        fuelDataTop.add("MAP/\nRPM");						
					}
		    		populateFuelTable(connectionService.getMapTwoData(), connectionService.isFuelMapTwoVE());
					break;
			}

			for (int x = 0; x < 16; x++) {
	        	fuelDataTop.add(String.format(STRING_FORMAT, (int) (x * MULTIPLIER)));
	        }
						
			lastRPM = 6000;
			lastMAP = 60;
			setCurrentCell();
		}	
	}
}
