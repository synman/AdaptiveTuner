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
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
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

public class MainActivity extends Activity implements ActionBar.TabListener {
	
	public static final String TAG = "Adaptive";
	public static final boolean DEBUG = false;

	private static final int LONG_PAUSE = 200;

	private static final int AFR_MIN = 970;
	private static final int AFR_MAX = 1970;

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
	
	private GaugeNeedle waterNeedle;
	private GaugeNeedle iatNeedle;
	private GaugeNeedle mapNeedle;
	private GaugeNeedle afrNeedle;
	private GaugeNeedle targetAfrNeedle;
	private GaugeNeedle rpmNeedle;
	
	private GaugeSlider tpsSlider;

	private ProgressDialog progress;
	
	private Handler refreshHandler;

	private final IntentFilter usbDetachedFilter = new IntentFilter();
	private final BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();;
	
	private ArrayAdapter<String> devices;
	private ArrayAdapter<String> dataArray;

	private static int lastRPM = 0;
	
	private SharedPreferences prefs ;
	private Context ctx;
	
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
                	if (itemPosition == 2) buildFuelMap();
               
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
        
        fuelDataTop.add("RPM");
        fuelDataTop.add("0 kPA");
        fuelDataTop.add("13 kPA");
        fuelDataTop.add("26 kPA");
        fuelDataTop.add("40 kPA");
        fuelDataTop.add("53 kPA");
        fuelDataTop.add("66 kPA");
        fuelDataTop.add("80 kPA");
        fuelDataTop.add("93 kPA");
        fuelDataTop.add("106 kPA");
        fuelDataTop.add("120 kPA");
        fuelDataTop.add("133 kPA");
        fuelDataTop.add("146 kPA");
        fuelDataTop.add("160 kPA");
        fuelDataTop.add("173 kPA");
        fuelDataTop.add("186 kPA");
        fuelDataTop.add("200 kPA");
        
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
        	if (savedTabIndex == 2) buildFuelMap();
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
					(connectionService != null && 
					connectionService.getState() != State.CONNECTED_BT &&
					connectionService.getState() != State.CONNECTED_USB)) {
		    	
    			if (menuConnect != null && menuConnect.getTitle().equals(getResources().getString(R.string.menu_disconnect))) {
    				menuConnect.setTitle(R.string.menu_connect);
    			}
				
    			if (menuUsbConnect != null && menuUsbConnect.getTitle().equals(getResources().getString(R.string.menu_disconnect))) {
    				menuUsbConnect.setTitle(R.string.menu_usb_connect);
    			}
				
    			if (connectionService != null && connectionService.getState() == State.DISCONNECTED) {
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
	        		populateFuelTable(connectionService.getMapData());

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

//			txtData.setText(String.format("AVG: %.0f ms - TPS: %d%%", connectionService.getAvgResponseMillis(), tps));
			txtData.setText(String.format("AVG: %.0f ms", connectionService.getAvgResponseMillis()));

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
    		
    		final float afrValF = afrVal;
    		final float targetAfrValF = targetAfrVal;

			afrNeedle.setValue(AFR_MAX - afrValF + AFR_MIN);
			targetAfrNeedle.setValue(AFR_MAX - targetAfrValF + AFR_MIN);
    		
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
    		
    		// dismiss the progress bar if it is visible
    		if (progress != null && progress.isShowing()) {
    			progress.dismiss();
    			if (menuConnect != null) menuConnect.setTitle(R.string.menu_disconnect);
    		}
    		
    		if (refreshHandler != null) refreshHandler.postDelayed(this, LONG_PAUSE);
        }
    };
    
	private void populateFuelTable(final StringBuffer data) {

		String[] map = data.toString().trim().split(" ");
		short cnt = 0;

		for (int x = 0; x < 32; x++) {
			fuelData.add(String.format("%d", x * 300));
			for (int y = 0; y < 16; y++) {
				double val = Double.parseDouble(String.format(Locale.US, "%.2f", Integer.parseInt(map[cnt] + map[cnt+1], 16) / 128f));
				fuelData.add(String.format("%.2f", val));

				if (DEBUG) Log.d(TAG, String.format("%d:%d = %.2f", x, y, val));
				cnt = (short) (cnt + 2);
			}
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
            		connectionService != null && connectionService.getState() == State.CONNECTED_USB) {// &&
//            		((UsbConnectedThread)connected).isUsbDevice((UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE))) {
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
				f.write("timestamp, rpm, map, closedloop, targetafr, afr, refafr, tps, wat, mat\n".getBytes());
				
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
				buildFuelMap();
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
	
	private void buildFuelMap() {
		if (ConnectionService.state != ConnectionService.State.DISCONNECTED) {
			mapMode = true;
	    	progress = ProgressDialog.show(ctx, "Fuel Map" , "Reading map values ...");
	    	progress.setCancelable(true);
	    	progress.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface arg0) {
					disconnect();
				}});
	    	
        	startService(new Intent(ConnectionService.ACTION_UPDATE_FUEL_MAP));	

        	fuelData.clear();      		
		} else {
			fuelData.clear();
			for (int x = 0; x < 32; x++) {
				fuelData.add(String.format("%d", x * 300));
				for (int y = 0; y < 16; y++) {
					fuelData.add("");
				}
			}	
		}
		
//		fuelData.clear();
//		
//		int y = 0;
//		int z = 300;
//		fuelData.add("0");
//		for (int x = 0 ; x < 512 ; x++) {
//			fuelData.add("000.00");
//
//			y++;
//			if (y==16 && z < 9600) {
//				y=0;
//				fuelData.add(String.format("%d", z));
//				z=z+300;
//			}
//		}
	}

	@Override
	public void onBackPressed() {

		if (layoutDevices.getVisibility() == View.VISIBLE) { 
			layoutDevices.setVisibility(View.INVISIBLE);
		} else {
			super.onBackPressed();
		}
	}

//	public void onClick(View arg0) {
////        Filter f = (Filter) v.getTag();
////        FilterFullscreenActivity.show(this, input, f);
//	}
	
//    class MyGestureDetector extends SimpleOnGestureListener {
//
//        final private ViewFlipper vf = (ViewFlipper) findViewById(R.id.gridFlipper);
//        
//        final private Animation animFlipInForeward = AnimationUtils.loadAnimation(ctx, R.anim.flipin);
//        final private Animation animFlipOutForeward = AnimationUtils.loadAnimation(ctx, R.anim.flipout);
//        final private Animation animFlipInBackward = AnimationUtils.loadAnimation(ctx, R.anim.flipin_reverse);
//        final private Animation animFlipOutBackward = AnimationUtils.loadAnimation(ctx, R.anim.flipout_reverse);
//        
//        @Override
//        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//            try {
//                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) return false;
//                
//                // right to left swipe
//                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
////                    Toast.makeText(ctx, "Left Swipe", Toast.LENGTH_SHORT).show();
//
////                    vf.setAnimation(AnimationUtils.loadAnimation(ctx, android.R.anim.slide_out_right));
//                    vf.setInAnimation(animFlipInForeward);
//                    vf.setOutAnimation(animFlipOutForeward);
//                    
//                    vf.showNext();
//                    
//                }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
////                    Toast.makeText(ctx, "Right Swipe", Toast.LENGTH_SHORT).show();
//                    
////                    vf.setAnimation(AnimationUtils.loadAnimation(ctx, android.R.anim.slide_in_left));
//                    vf.setInAnimation(animFlipInBackward);
//                    vf.setOutAnimation(animFlipOutBackward);
//                    
//                    vf.showPrevious();                    
//                }
//            } catch (Exception e) {
//                // nothing
//            }
//            return false;
//        }
//
//    }

}
