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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.Engine;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.TypedValue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.shellware.adaptronic.adaptive.tuner.changelog.ChangeLog;
import com.shellware.adaptronic.adaptive.tuner.logging.AdaptiveLogger;
import com.shellware.adaptronic.adaptive.tuner.logging.AdaptiveLogger.Level;
import com.shellware.adaptronic.adaptive.tuner.preferences.AdaptivePreferences;
import com.shellware.adaptronic.adaptive.tuner.receivers.BatteryStatusReceiver;
import com.shellware.adaptronic.adaptive.tuner.services.ConnectionService;
import com.shellware.adaptronic.adaptive.tuner.services.ConnectionService.State;
import com.shellware.adaptronic.adaptive.tuner.valueobjects.LogItems.LogItem;
import com.shellware.adaptronic.adaptive.tuner.widgets.AdaptiveAdapter;
import com.shellware.adaptronic.adaptive.tuner.widgets.CellValueWidget;
import com.shellware.adaptronic.adaptive.tuner.widgets.GaugeNeedle;
import com.shellware.adaptronic.adaptive.tuner.widgets.GaugeSlider;

public class MainActivity 	extends Activity 
							implements ActionBar.TabListener, 
									   OnClickListener {
	
	private static final int LONG_PAUSE = 500;
	private static final int SHORT_PAUSE = 200;

	private static final int AFR_MIN = 800;
	private static final int AFR_MAX = 1800;

	
	private static final float VE_DIVISOR = 128f;
	private static final float MS_DIVISOR = 1500f;
	
	private static final String LOG_HEADER = "timestamp, rpm, map, closedloop, targetafr, afr, refafr, tps, wat, mat, auxt, knock, volts\n";
	
	private final Fragment[] frags = { null, null, null };
	private static final short FRAGS_COUNT = 3;
	
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
	
	private ImageView crossXYellow;
	private ImageView crossYYellow;
	private ImageView crossXGreen;
	private ImageView crossYGreen;
	
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
	private AdaptiveAdapter dataArray;

	private static int lastMAP = 0;
	private static int lastRPM = 0;
	private static int lastTPS = 0;
	
	private static boolean lastClosedLoop = false;
	
	private SharedPreferences prefs ;
	private Context ctx;
	
	private static int screenWidth = 0;
//	private static int screenHeight = 0;
	
	private static int tempUomPref = 1;
	private static boolean displayAuxTPref = false;
	
	private static boolean wakeLock = true;

	private static boolean afrNotEqualTargetPref = false;
	private static float afrNotEqualTargetTolerance = 5f;
	
	private static boolean waterTempPref = false;
	private static float minimumWaterTemp = 0f;
	private static float maximumWaterTemp = 210f;

	private static boolean showBoost = true;
	private static int maxBoost = 0;

	private static boolean autoConnect = false;
	private static boolean shuttingDown = false;
	
	private static boolean mapMode = false;
	public static boolean mapReady = false;
	
	private static boolean crankMode = false;
	public static boolean crankReady = false;
	
	private static boolean saveMode = false;
	public static boolean saveReady = false;
	private static String saveName = "";
	
	private static boolean logAll = false;
	private static boolean afrAlarmLogging = false;
	
	private GridView fuelGridHeaderTop;
	private GridView fuelGrid;

	private RadioButton radioMapOne;
	private RadioButton radioMapTwo;
	private RadioButton radioMapCrank;
	
	private ArrayAdapter<String> fuelDataTop;
	private ArrayAdapter<String> fuelData;

	SimpleOnGestureListener simpleOnGestureListener = new SimpleOnGestureListener() {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    	int itemPosition = getActionBar().getSelectedNavigationIndex();
                        if (diffX > 0) {
                        	if (itemPosition == 0) itemPosition = 3;                        	
                        	itemPosition--;
                        } else {
                        	if (itemPosition == 2) itemPosition = -1;                        	
                        	itemPosition++;
                        }
                    	getActionBar().setSelectedNavigationItem(itemPosition);                    	
                    }
                    result = true;
                } 
                else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            // do nothing
                        } else {
                            //do nothing
                        }
                    }
//                    result = true;

            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }
	};
	
   private GestureDetector gestureDetector = new GestureDetector(simpleOnGestureListener);
    
   @Override
   public boolean onTouchEvent(MotionEvent event) {
    // TODO Auto-generated method stub
      return gestureDetector.onTouchEvent(event);
   }
		
    // TextToSpeech
    private static TextToSpeech speaker;
	private static AudioManager am;
	
	private static String AUDIBLE_MAX_RPM;
	private static String AUDIBLE_MAX_MAP;
	private static String AUDIBLE_MAX_WAT;
	private static String AUDIBLE_MAX_MAT;
	private static String AUDIBLE_MAX_TPS;
	
	private static boolean doAudibles = false;

	private static int audibleMaxRpm;
	private static int audibleMaxMap;
	private static int audibleMaxWat;
	private static int audibleMaxMat;
	private static int audibleMaxTps;
	
	private static final int TTS_MIN_SILENCE_MILLIS = 15000;
	
    private static boolean isTtsReady = false;
    private static long lastPhraseMillis = 0;
    private static final AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
        	// do nothing
        }
    };

	private ConnectionService connectionService;
	private ServiceConnection connectionServiceConnection;
	
	private BatteryStatusReceiver batteryStatusReceiver;
	
//	@Override
//	public void onAttachedToWindow() {
//	    super.onAttachedToWindow();
//	    Window window = getWindow();
//	    window.setFormat(PixelFormat.RGBA_8888);
//	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
        ctx = this;
    	prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	
    	// Instantiate all of our fragments
        frags[0] = getFragmentManager().findFragmentById(R.id.frag_adaptive);
        frags[1] = getFragmentManager().findFragmentById(R.id.frag_gauges);
        frags[2] = getFragmentManager().findFragmentById(R.id.frag_fuel);
                
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
                	
                	for (int x = 0; x < FRAGS_COUNT; x++) {
                		ft.hide(frags[x]);
                	}
                	
                	//kludge to populate fuel map
                	if (itemPosition == 2) getFuelMaps();
               
                	ft.show(frags[itemPosition]);
                	ft.commit();

                	// don't allow the fuel map to be saved
                	if (itemPosition != 2) {
	        			Editor edit = prefs.edit();
	        			edit.putInt("prefs_last_tab",itemPosition);
	        			edit.commit();
                	}
                	
//            		dataArray.setDash(!dataArray.isDash());
//            		dataArray.clear();
//            		
//                    dataArray.add("RPM\n9000");
//                    dataArray.add("MAP\n175");
//                    dataArray.add("MAT\n65\u00B0");
//                    dataArray.add("AFR\n14.7 (14.7)");
//                    dataArray.add("TAFR\n14.7");
//                    dataArray.add("WAT\n185\u00B0");
//                    dataArray.add("TPS\n90%");
//                    dataArray.add("KNOCK\n0");
//                    dataArray.add("BAT\n14.1v");

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
        
//        dataArray = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        dataArray = new AdaptiveAdapter(this, R.layout.adaptive_grid_item,
        								Typeface.createFromAsset(ctx.getAssets(), "fonts/digital_7.ttf"));
        
        dataArray.add("RPM\n ----");
        dataArray.add("MAP\n ---");
        dataArray.add("MAT\n ---\u00B0");

        if (displayAuxTPref) dataArray.add("AUXT\n ---\u00B0");

        dataArray.add("AFR\n --.- (--.-)");
        dataArray.add("TAFR\n --.-");
        dataArray.add("WAT\n ---\u00B0");

        dataArray.add("TPS\n---%");
        dataArray.add("KNOCK\n---");
        dataArray.add("BAT\n--.-v");
 
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
        rpmNeedle.setMaxValue(10000);
        rpmNeedle.setMinDegrees(-180);
        rpmNeedle.setMaxDegrees(90); 
        
        tpsSlider.setMinValue(0);
        tpsSlider.setMaxValue(100);
        tpsSlider.setSuffix("%");
        
        //tps text
       TextView tpsTitle = (TextView) findViewById(R.id.tpstitle);
       tpsTitle.setTypeface(Typeface.createFromAsset(ctx.getAssets(), "fonts/digital_7.ttf"));
        
        afrGaugeAlarm = (ImageView) findViewById(R.id.afrmeteralarm);
        waterGaugeAlarm = (ImageView) findViewById(R.id.watermeteralarm);
        
        ChangeLog cl = new ChangeLog(this);
        if (cl.firstRun()) {
            cl.getLogDialog().show();
        }
        
        fuelGrid = (GridView) findViewById(R.id.gridFuel);
        fuelData = new ArrayAdapter<String>(this, R.layout.tiny_list_item);
        fuelGrid.setAdapter(fuelData);
        
        // lotsa stuff happens here
        fuelGrid.setOnItemLongClickListener(cellEditListener);
        fuelGrid.setOnTouchListener(new OnTouchListener () {

			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				return gestureDetector.onTouchEvent(event);
			}
		});
        
        fuelGridHeaderTop = (GridView) findViewById(R.id.gridFuelHeaderTop);
        fuelDataTop = new ArrayAdapter<String>(this, R.layout.tiny_list_item_bold);
        fuelGridHeaderTop.setAdapter(fuelDataTop);
        
        crossXYellow = (ImageView) findViewById(R.id.crossXYellow);
        crossYYellow = (ImageView) findViewById(R.id.crossYYellow);
        
        crossXGreen = (ImageView) findViewById(R.id.crossXGreen);
        crossYGreen = (ImageView) findViewById(R.id.crossYGreen);
        
        // initialize it just to be safe
        crossX = crossXYellow;
        crossY = crossYYellow;
        
        fuelTableAfr = (TextView) findViewById(R.id.fuelTabAfr);
        
        radioMapOne = (RadioButton) findViewById(R.id.radioMapOne);
        radioMapTwo = (RadioButton) findViewById(R.id.radioMapTwo);
        radioMapCrank = (RadioButton) findViewById(R.id.radioMapCrank);
        
        radioMapOne.setOnClickListener(this);
        radioMapTwo.setOnClickListener(this);
        radioMapCrank.setOnClickListener(this);
        
        usbDetachedFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, usbDetachedFilter);

        // kludge to ensure we're not stuck in a charge loop
        Editor edit = prefs.edit();
        edit.putBoolean("prefs_connect_on_charge_waiting", false);
        edit.commit();
        
	    batteryStatusReceiver = new BatteryStatusReceiver();
	    registerReceiver(batteryStatusReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		connectionServiceConnection = new ServiceConnection() {
		    public void onServiceConnected(ComponentName className, IBinder service) {
		        connectionService = ((ConnectionService.ServiceBinder) service).getService();
		        AdaptiveLogger.log("service bound");
		    }

		    public void onServiceDisconnected(ComponentName className) {
		        connectionService = null;
		        AdaptiveLogger.log("service unbound");
		    }
		};
		
	    bindService(new Intent(this, ConnectionService.class), connectionServiceConnection, Context.BIND_AUTO_CREATE);	
	    
        // TextToSpeech - SMS
	    am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		speaker = new TextToSpeech(this, new OnInitListener() {
			public void onInit(int status) {

				speaker.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
					public void onUtteranceCompleted(String utteranceId) {
						am.abandonAudioFocus(afChangeListener);
					}
				});

				AUDIBLE_MAX_MAP = getResources().getString(R.string.audible_max_map);
				AUDIBLE_MAX_MAT = getResources().getString(R.string.audible_max_mat);
				AUDIBLE_MAX_RPM = getResources().getString(R.string.audible_max_rpm);
				AUDIBLE_MAX_TPS = getResources().getString(R.string.audible_max_tps);
				AUDIBLE_MAX_WAT = getResources().getString(R.string.audible_max_wat);

                isTtsReady = true;
			}
        });		
	}
    
    @Override
	protected void onResume() {
    	super.onResume();

    	// set screen dimensions
    	screenWidth = getWindowManager().getDefaultDisplay().getWidth();
//    	screenHeight = getWindowManager().getDefaultDisplay().getHeight();

    	// initialize our preferences
    	doAudibles = prefs.getBoolean("prefs_audibles_pref", false);
    	
    	audibleMaxMap = prefs.getInt("prefs_audibles_max_map", 150);
    	audibleMaxMat = prefs.getInt("prefs_audibles_max_mat", 120);
    	audibleMaxRpm = prefs.getInt("prefs_audibles_max_rpm", 7000);
    	audibleMaxTps = prefs.getInt("prefs_audibles_max_map", 80);
    	audibleMaxWat = prefs.getInt("prefs_audibles_max_wat", 200);
    	
    	tempUomPref = Integer.parseInt(prefs.getString("prefs_uom_temp", "1"));
    	if (connectionService != null) connectionService.setTempUomPref(tempUomPref);

    	displayAuxTPref = prefs.getBoolean("prefs_show_auxt", false);
    	showBoost = prefs.getBoolean("prefs_show_boost", false);
    	maxBoost = Integer.parseInt(prefs.getString("prefs_max_boost", "0"));

    	if (showBoost) {
    		switch (maxBoost) {
    			case 0:
    	            ((ImageView) findViewById(R.id.map)).setImageResource(R.drawable.boost_vac_15);
    	            break;
    			case 1:
    	            ((ImageView) findViewById(R.id.map)).setImageResource(R.drawable.boost_vac);
    	            break;
    		}
        } else {
            ((ImageView) findViewById(R.id.map)).setImageResource(R.drawable.boost_vac);        	
        }
               
    	wakeLock = prefs.getBoolean("prefs_wake_lock", true);
    	if (connectionService != null) connectionService.setWakeLock(wakeLock);
    	
    	afrNotEqualTargetPref = prefs.getBoolean("prefs_afrnottarget_pref", false);
    	
    	afrNotEqualTargetTolerance = prefs.getFloat("prefs_afrnottarget_tolerance_pref", 5f);
    	if (connectionService != null) connectionService.setAfrNotEqualTargetTolerance(afrNotEqualTargetTolerance);
    	
    	waterTempPref = prefs.getBoolean("prefs_watertemp_pref", false);    	
//    	remoteName = prefs.getString("prefs_remote_name", "");
//    	remoteMacAddr = prefs.getString("prefs_remote_mac", "");    	
    	autoConnect = prefs.getBoolean("prefs_auto_connect", false);
    	
    	// need to set gauge scale based on uom selected
    	switch (tempUomPref) {
    		case 0:
    	    	minimumWaterTemp = prefs.getFloat("prefs_min_water_temp", AdaptivePreferences.MIN_WATER_TEMP_CELSIUS);
    	    	maximumWaterTemp = prefs.getFloat("prefs_max_water_temp", AdaptivePreferences.MAX_WATER_TEMP_CELSIUS);

    	        iatNeedle.setMaxValue(100);
    	        imgIat.setImageResource(R.drawable.iatgauge_celsius);
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

    	for (int x = 0; x < FRAGS_COUNT; x++) {
    		ft.hide(frags[x]);
    	}

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
        	AdaptiveLogger.log("USB Device Attached");
        	Intent intent = new Intent(ConnectionService.ACTION_CONNECT_USB);
        	intent.setPackage(ctx.getPackageName());
        	startService(intent);	
        }   	

        // refresh the screen (atleast) once
        refreshHandler = new Handler();
		refreshHandler.postDelayed(RefreshRunnable, LONG_PAUSE);
		
		Intent intent = new Intent(ConnectionService.ACTION_UI_ACTIVE);
		intent.setPackage(ctx.getPackageName());
		startService(intent);	}

    @Override
    protected void onPause() {
    	super.onPause();

    	refreshHandler.removeCallbacks(RefreshRunnable);
    	refreshHandler = null;
    	
    	if (!shuttingDown) {
    		Intent intent = new Intent(ConnectionService.ACTION_UI_INACTIVE);
    		intent.setPackage(ctx.getPackageName());
    		startService(intent);
    	}
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();

		unregisterReceiver(usbReceiver);
		unregisterReceiver(batteryStatusReceiver);

		speaker.stop();
		speaker.shutdown();
		
		if (shuttingDown) {
			stopService(new Intent(this, ConnectionService.class));
		}
		
    	unbindService(connectionServiceConnection);
	}

	private final Runnable RefreshRunnable = new Runnable() {

		public void run() {

			// bail if no service binding available or not connected
			if (connectionService == null || 
					(connectionService.getState() != State.CONNECTED_BT &&
					connectionService.getState() != State.CONNECTED_USB)) {
		    	
    			if (connectionService != null && connectionService.getState() == State.DISCONNECTED) {
    				
        			imgStatus.setBackgroundColor(Color.TRANSPARENT);

        			if (progress != null && progress.isShowing()) {
    					progress.dismiss();
    				}
    				if (autoConnect && prefs.getString("prefs_remote_mac", "").length() > 0) {
			    		disconnect();
			    		bluetoothConnect(prefs.getString("prefs_remote_name", ""),  prefs.getString("prefs_remote_mac", ""));
			    		
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
	    	
	    	// show our cranking table (because we asked for it)
	    	if (crankMode) {
	    		if (crankReady) {
	    			if (connectionService != null) {
		    			// do a couple resets
	    				fuelData.clear();

	    				fuelGrid.setNumColumns(4);
		    			fuelGridHeaderTop.setVisibility(View.INVISIBLE);

		    			final String[] map = connectionService.getCrankData().toString().split(" ");
	    				short cnt = 0;
	    				short temp = -30;
	    				
	    				//TODO: this is currently broke (work in progress)
	    				while (cnt < 30) {	    					
	    					final double val = Double.parseDouble(String.format(Locale.US, "%.2f", 
									Integer.parseInt(map[cnt] + map[cnt+1], 16) / 
									(connectionService.isCrankMapVE() ? VE_DIVISOR : MS_DIVISOR)));

	    					fuelData.add(String.format("%d\u00B0", temp));
	    					fuelData.add(String.format("%d", Math.round(val)));
	    					
	    					final double val2 = Double.parseDouble(String.format(Locale.US, "%.2f", 
									Integer.parseInt(map[cnt+30] + map[cnt+31], 16) / 
									(connectionService.isCrankMapVE() ? VE_DIVISOR : MS_DIVISOR)));

	    					fuelData.add(String.format("%d\u00B0", temp + 80));
	    					fuelData.add(String.format("%d", Math.round(val2)));	
	    					
	    					cnt = (short) (cnt + 2);
	    					temp = (short) (temp + 5);
	    				}
	    			}
	    			
	    			crankMode = false;
	    			crankReady = false;
	    		}
	    		if (refreshHandler != null) refreshHandler.postDelayed(this, LONG_PAUSE);
        		return;
	    	}
	    	
	    	// ecu file save in progress?
	    	if (saveMode) {
	    		if (saveReady) {
	    			saveMode = false;
	    			saveReady = false;
	    			
					Intent share = new Intent(Intent.ACTION_SEND);
					share.setType("text/plain");
					share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + saveName));
					startActivity(Intent.createChooser(share, getText(R.string.share_ecu_file_heading)));
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
			final int auxt = item.getAuxt();
			final int map = item.getMap(); lastMAP = map;
			
			final int rpm = item.getRpm();
			final int knock = item.getKnock();
			
			final float afr = item.getAfr();
			final float targetAfr = item.getTargetAfr();
			final float referenceAfr = item.getReferenceAfr();
			final float volts = item.getVolts();

			float afrVal = afr * 100;
    		float targetAfrVal = targetAfr * 100;
    		
    		if (afrVal > AFR_MAX) afrVal = AFR_MAX;
    		if (afrVal < AFR_MIN) afrVal = AFR_MIN;
    		
    		if (targetAfrVal > AFR_MAX) targetAfrVal = AFR_MAX;
    		if (targetAfrVal < AFR_MIN) targetAfrVal = AFR_MIN;

			if (closedLoop != lastClosedLoop) {
				targetAfrNeedle.setImageResource(closedLoop ? R.drawable.needle_middle_green : R.drawable.needle_middle_yellow);
		    	
		    	// set the fuel map crosshairs color based on closed loop status
				crossX.setVisibility(View.INVISIBLE);
				crossY.setVisibility(View.INVISIBLE);

				crossX = closedLoop ? crossXGreen : crossXYellow;
		        crossY = closedLoop ? crossYGreen : crossYYellow;

		        crossX.setVisibility(View.VISIBLE);
				crossY.setVisibility(View.VISIBLE);
				
				crossY.setX((fuelGrid.getChildAt(0) != null ? ((TextView) fuelGrid.getChildAt(0)).getWidth() : 6) - 5);
				crossX.setY(fuelGrid.getY() - 5);
				
				lastClosedLoop = closedLoop;
			}
			    		
    		if (rpm >= 200) lastRPM = rpm;

    		txtData.setText(String.format("AVG: %.0f ms", connectionService.getAvgResponseMillis()));

			if (frags[0].isVisible()) {
				imgFWait.setBackgroundColor(fWait ? Color.parseColor("#FFCC00") : Color.TRANSPARENT);
				imgFRpm.setBackgroundColor(fRpm ? Color.GREEN : Color.TRANSPARENT);
				imgFLoad.setBackgroundColor(fLoad ? Color.GREEN : Color.TRANSPARENT);
				
				imgIWait.setBackgroundColor(iWait ? Color.parseColor("#FFCC00") : Color.TRANSPARENT);
				imgIRpm.setBackgroundColor(iRpm ? Color.GREEN : Color.TRANSPARENT);
				imgILoad.setBackgroundColor(iLoad ? Color.GREEN : Color.TRANSPARENT);
	
				txtFuelLearn.setBackgroundColor(closedLoop ? Color.GREEN : Color.TRANSPARENT);

				dataArray.add(String.format("RPM\n%d", lastRPM));
	    		dataArray.add(String.format("MAP\n%d kPa", map));
	    		dataArray.add(String.format("MAT\n%d\u00B0 %s", mat, getTemperatureSymbol()));

	    		if (displayAuxTPref) dataArray.add(String.format("AUXT\n%d\u00B0 %s", auxt, getTemperatureSymbol()));

	    		dataArray.add(String.format("AFR\n%.1f (%.1f)", afr, referenceAfr));
	    		dataArray.add("TAFR\n" +  (targetAfr != 0f ? String.format("%.1f", targetAfr) : "--.-"));
	    		dataArray.add(String.format("WAT\n%d\u00B0 %s", wat, getTemperatureSymbol()));
	    		
	    		dataArray.add(String.format("TPS\n%d%%", lastTPS));
	    		dataArray.add(String.format("KNOCK\n%d", knock));
	    		dataArray.add(String.format("BAT\n%.1fv", volts));
	    		
				if (gridData.getChildAt(3) != null && gridData.getChildAt(5) != null) gridData.getChildAt(5).setBackgroundColor(Color.TRANSPARENT);
	    		if (gridData.getChildAt(3) != null) gridData.getChildAt(3).setBackgroundColor(Color.TRANSPARENT);
			}

			if (frags[1].isVisible()) {
				rpmNeedle.setValue(lastRPM); 
				tpsSlider.setValue(tps);			
				iatNeedle.setValue(mat); 
				waterNeedle.setValue(convertWat(wat));

				if (!showBoost) {
			        mapNeedle.setMinValue(0);
			        mapNeedle.setMaxValue(200);
			        mapNeedle.setMinDegrees(-150);
			        mapNeedle.setMaxDegrees(140);

			        mapNeedle.setValue(map);
				} else {				
			    	//TODO: formula for map to psi (MAP - 102) / 6.894 
			    	//TODO: formula for psi to inhg  * 2.036025
			        mapNeedle.setMinValue(0);
					
					if (map < 102) {
				        mapNeedle.setMaxValue(30);
				        mapNeedle.setMinDegrees(-180);
				        mapNeedle.setMaxDegrees(-90);

				        mapNeedle.setValue(30f + (((map - 102) / 6.894f) * 2.036025f));						
					} else {
				        mapNeedle.setMaxValue(maxBoost == 0 ? 15 : 30);
				        mapNeedle.setMinDegrees(-90);
				        mapNeedle.setMaxDegrees(90);

				        mapNeedle.setValue((map - 102) / 6.894f);
					}
				}
				
				afrNeedle.setValue(afrVal);
				targetAfrNeedle.setValue(targetAfrVal);
				
				waterGaugeAlarm.setBackgroundColor(Color.TRANSPARENT);
				afrGaugeAlarm.setBackgroundColor(Color.TRANSPARENT);
			}

			if (frags[2].isVisible()) {
				fuelTableAfr.setText(String.format("AFR: %.1f (%.1f)", afr, referenceAfr));

				// fuel map crosshairs
	    		setCurrentCell();

	    		fuelTableAfr.setBackgroundColor(Color.TRANSPARENT);
			}

			// water temperature alarm			
    		if (waterTempPref) {
    			if (wat < minimumWaterTemp) {
    				if (frags[0].isVisible() && gridData.getChildAt(3) != null && gridData.getChildAt(5) != null) gridData.getChildAt(5).setBackgroundColor(Color.BLUE);
    				if (frags[1].isVisible()) waterGaugeAlarm.setBackgroundColor(Color.BLUE);
    			}
    			if (wat > maximumWaterTemp) {
    				if (frags[0].isVisible() && gridData.getChildAt(3) != null && gridData.getChildAt(5) != null) gridData.getChildAt(5).setBackgroundColor(Color.RED);
    				if (frags[1].isVisible()) waterGaugeAlarm.setBackgroundColor(Color.RED);
    			}
    		}

    		// afr vs target alarm			
    		if (afrNotEqualTargetPref) {
    			final float threshold = targetAfr * (afrNotEqualTargetTolerance * .01f);
    			if (Math.abs(targetAfr - afr) >= threshold ) {
    				final int color =  afr > targetAfr ? Color.RED : Color.BLUE;
    				if (frags[0].isVisible() && gridData.getChildAt(3) != null) gridData.getChildAt(3).setBackgroundColor(color);
    				if (frags[1].isVisible()) afrGaugeAlarm.setBackgroundColor(color);
    				if (frags[2].isVisible()) fuelTableAfr.setBackgroundColor(color);
    			}
    		}
    		
    		// audibles
    		if (doAudibles) {
    			if (lastRPM > audibleMaxRpm) speakPhrase(String.format(AUDIBLE_MAX_RPM, lastRPM));
    			if (lastTPS > audibleMaxTps) speakPhrase(String.format(AUDIBLE_MAX_TPS, lastTPS));
    			if (mat > audibleMaxMat) speakPhrase(String.format(AUDIBLE_MAX_MAT, mat));
    			if (map > audibleMaxMap) speakPhrase(String.format(AUDIBLE_MAX_MAP, map));
				if (wat > audibleMaxWat) speakPhrase(String.format(AUDIBLE_MAX_WAT, wat));
    		}
    		
    		// dismiss the progress bar if it is visible
    		if (progress != null && progress.isShowing()) {
    			progress.dismiss();
    		}
    		
    		if (refreshHandler != null) refreshHandler.postDelayed(this, SHORT_PAUSE);
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
    		AdaptiveLogger.log(Level.ERROR, "Unknown exception thrown in setCurrentCell - " + ex.getMessage());
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
	
					AdaptiveLogger.log(String.format(Locale.US, "fuel table %d:%d = %.2f", x, y, val));
					cnt = (short) (cnt + 2);
				}
			}			
		} catch (Exception ex) {
    		AdaptiveLogger.log(Level.ERROR, "Unknown exception thrown in populateFuelTable - " + ex.getMessage());
		}
	}
	
	private void populateCrankTable() {
    	crankReady = false;
    	crankMode = true;
    	
		Intent intent = new Intent(ConnectionService.ACTION_READ_CRANKING_MAP);
		intent.setPackage(ctx.getPackageName());
		startService(intent);
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
    			AdaptiveLogger.log("USB Device Detached");
  
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
            bluetoothConnect(name, address);
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
    		AdaptiveLogger.log(Level.ERROR, "Unknown exception thrown in showDevices - " + ex.getMessage());
    	}
    	
    	if (devices.getCount() > 0) layoutDevices.setVisibility(View.VISIBLE);
    }
    
    private void bluetoothConnect(final String name, final String macAddr) {
    	
    	if (progress != null && progress.isShowing()) return;
    	
    	Intent service = new Intent(ConnectionService.ACTION_CONNECT_BT);
    	service.setPackage(ctx.getPackageName());
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
		
    	if (progress != null && progress.isShowing()) progress.dismiss();
    	imgStatus.setBackgroundColor(Color.TRANSPARENT);				

    	Intent intent = new Intent(ConnectionService.ACTION_DISCONNECT);
    	intent.setPackage(ctx.getPackageName());
    	startService(intent);
    }
    
    @SuppressWarnings("unused")
	private static void sleep(final int millis) {
    	try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
    		AdaptiveLogger.log(Level.ERROR, "Interupted exception thrown in sleep - " + e.getMessage());
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        
        myMenu = menu;
        menuShareLog = myMenu.findItem(R.id.menu_share);
    	menuConnect = myMenu.findItem(R.id.menu_connect);
    	menuUsbConnect = myMenu.findItem(R.id.menu_usb_connect);
    	
    	return onPrepareOptionsMenu(menu);
    }
    

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		
		myMenu.findItem(R.id.menu_save_map).setEnabled(false);
		myMenu.findItem(R.id.menu_fuel_cut).setEnabled(false);

		menuConnect.setEnabled(false);
		menuUsbConnect.setEnabled(false);

		menuShareLog.setVisible(false);

		// show share button if logging
    	if (connectionService != null) {
    		menuShareLog.setVisible((afrAlarmLogging && !connectionService.getAfrAlarmLogItems().getItems().isEmpty()) ||
					 (logAll && !connectionService.getLogAllItems().getItems().isEmpty()));
    		
    		myMenu.findItem(R.id.menu_fuel_cut).setCheckable(true);
    		myMenu.findItem(R.id.menu_fuel_cut).setChecked(prefs.getBoolean("prefs_fuel_cut", false));	

    		switch (connectionService.getState()) {
    			case DISCONNECTED:
		    		menuConnect.setTitle(R.string.menu_connect);
		    		menuUsbConnect.setTitle(R.string.menu_usb_connect);
		    		menuConnect.setEnabled(true);
		    		menuUsbConnect.setEnabled(true);
		    				    		
		    		break;
	    	
    			case CONNECTED_BT:
    	    		myMenu.findItem(R.id.menu_save_map).setEnabled(true);
    	    		myMenu.findItem(R.id.menu_fuel_cut).setEnabled(true);
    				
	    			menuConnect.setTitle(R.string.menu_disconnect);
	        		menuConnect.setEnabled(true);
	        		menuUsbConnect.setEnabled(false);
	        		
	        		break;

    			case CONNECTED_USB:
    	    		myMenu.findItem(R.id.menu_save_map).setEnabled(true);
    	    		myMenu.findItem(R.id.menu_fuel_cut).setEnabled(true);

    	    		menuUsbConnect.setTitle(R.string.menu_disconnect);
            		menuConnect.setEnabled(false);
            		menuUsbConnect.setEnabled(true);
            		
            		break;
    				
    			default:
    				break;

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
	        	}
	            return true;
	            
	        case R.id.menu_usb_connect:
	        	Intent intent = new Intent(ConnectionService.ACTION_CONNECT_USB);
	        	intent.setPackage(ctx.getPackageName());
	        	startService(intent);
	        	return true;
	        	
	        case R.id.menu_prefs:
                startActivity(new Intent(this, AdaptivePreferences.class));
                return true;
                
	        case R.id.menu_save_map:	        	
	        	saveMode = true;

	        	final File sdcard = Environment.getExternalStorageDirectory();
				final File dir = new File (sdcard.getAbsolutePath() + "/AdaptiveTuner/");
				dir.mkdirs();
				
				saveName = dir.getAbsolutePath() + "/" + new SimpleDateFormat("yyyyMMdd_HHmmss'.ecu'", Locale.US).format(new Date());

				progress = ProgressDialog.show(ctx, getResources().getString(R.string.save_ecu_title), 
	        										getResources().getString(R.string.save_ecu_message) + " " + saveName + " ...");
		    	progress.setCancelable(false);

		    	final Intent sm = new Intent(ConnectionService.ACTION_SAVE_ECU_FILE);
		    	sm.setPackage(ctx.getPackageName());
	        	sm.putExtra("map_filename", saveName);
		    	
	        	startService(sm);	
	        	return true;
                
	        case R.id.menu_info:
	        	final String info = String.format(Locale.US, "Map 1 VE: %d Map 2 VE: %d Crank Map VE: %d Mode: %d RPM Step: %d Max MAP: %d Fuel Trim: %d Ign Trim: %d",
						        								connectionService.isFuelMapOneVE() ? 1 : 0,
						        								connectionService.isFuelMapTwoVE() ? 1 : 0,
						        								connectionService.isCrankMapVE() ? 1 : 0,
						        								connectionService.getTuningMode(),
						        								connectionService.getRpmStepSize(),
						        								connectionService.getMaxMapValue(),
						        								connectionService.getFuelTrim(),
						        								connectionService.getIgnitionTrim());

	        	Toast.makeText(getApplicationContext(), info, Toast.LENGTH_LONG).show();
	        	return true;

	        case R.id.menu_fuel_cut:
	        	if (connectionService != null && connectionService.getState() != State.DISCONNECTED) {
	        		if (item.isChecked()) {
	        			connectionService.updateRegister((short) ConnectionService.REGISTER_2052_MASTER_TRIM, (short) 0);	        		
		        	} else {
						connectionService.updateRegister((short) ConnectionService.REGISTER_2052_MASTER_TRIM, (short) 156);
		        	}
	        	}
	        	
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
				AdaptiveLogger.log("Saving log as: " + logLocation);

				Intent share = new Intent(Intent.ACTION_SEND);
				share.setType("text/plain");
				share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.getPath()));
				startActivity(Intent.createChooser(share, getText(R.string.share_afr_log_heading)));

			} catch (Exception e) {
	    		AdaptiveLogger.log(Level.ERROR, "Unknown exception thrown in AFR shareLog - " + e.getMessage());
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
				AdaptiveLogger.log("Saving log as: " + logLocation);

				Intent share = new Intent(Intent.ACTION_SEND);
				share.setType("text/plain");
				share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.getPath()));
				startActivity(Intent.createChooser(share, getText(R.string.share_log_all_heading)));

			} catch (Exception e) {
	    		AdaptiveLogger.log(Level.ERROR, "Unknown exception thrown in setCurrentCell");
				Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}

	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		
//		dataArray.setDash(!dataArray.isDash());
//		dataArray.clear();
//		
//        dataArray.add("RPM\n9000");
//        dataArray.add("MAP\n175");
//        dataArray.add("MAT\n65\u00B0");
//        dataArray.add("AFR\n14.7 (14.7)");
//        dataArray.add("TAFR\n14.7");
//        dataArray.add("WAT\n185\u00B0");
//        dataArray.add("TPS\n90%");
//        dataArray.add("KNOCK\n0");
//        dataArray.add("BAT\n14.1v");

//		ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
		ft.show(frags[tab.getPosition()]);
	}
	
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {

//		ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
		ft.hide(frags[tab.getPosition()]);
	}
	
	
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		
//		ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
		if (tab.getPosition() == 2) getFuelMaps();
		ft.show(frags[tab.getPosition()]);
		
		// we don't want to overwrite our pref if we're in onCreate
		// also don't allow fuel map to be set as default
		if (lvDevices != null && tab.getPosition() != 2) {
			Editor edit = prefs.edit();
			edit.putInt("prefs_last_tab", tab.getPosition());
			edit.commit();
		}
	}
	
	private void getFuelMaps() {
    	fuelData.clear();      		

    	if (ConnectionService.state != ConnectionService.State.DISCONNECTED) {
			mapMode = true;
	    	progress = ProgressDialog.show(ctx, getResources().getString(R.string.read_fuel_map_title), 
	    										getResources().getString(R.string.read_fuel_map_message) + " ...");
	    	progress.setCancelable(false);
	    	
        	Intent intent = new Intent(ConnectionService.ACTION_READ_FUEL_MAP);
        	intent.setPackage(ctx.getPackageName());
        	startService(intent);	
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
			
			// do a couple resets
			fuelGrid.setNumColumns(17);
			fuelGridHeaderTop.setVisibility(View.VISIBLE);
			radioMapCrank.setChecked(false);

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
			
	    	// set the fuel map crosshairs color based on closed loop status
			crossX.setVisibility(View.INVISIBLE);
			crossY.setVisibility(View.INVISIBLE);

			crossX = lastClosedLoop ? crossXGreen : crossXYellow;
	        crossY = lastClosedLoop ? crossYGreen : crossYYellow;

	        crossX.setVisibility(View.VISIBLE);
			crossY.setVisibility(View.VISIBLE);
			
			crossY.setX((fuelGridHeaderTop.getChildAt(0) != null ? ((TextView) fuelGridHeaderTop.getChildAt(0)).getWidth() : 6) - 5);
			crossY.setY(fuelGrid.getY());
			
			crossX.setX(fuelGridHeaderTop.getChildAt(0) != null ? ((TextView) fuelGridHeaderTop.getChildAt(0)).getWidth() : 6);
			crossX.setY(fuelGrid.getY() - 5);
			
//			lastRPM = 1500;
//			lastMAP = 60;
//			setCurrentCell();
		} else {
			if (view.getId() == R.id.radioMapCrank) {
				radioMapOne.setChecked(false);
				radioMapTwo.setChecked(false);
				radioMapCrank.setChecked(true);
				populateCrankTable();
			}
		}
	}
	
	private OnItemLongClickListener cellEditListener = new OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

			// bail if fuel grid not active
			if (getActionBar().getSelectedNavigationIndex() != 2) return true;

			// bail if not connected
			if (connectionService == null || connectionService.getState() == State.DISCONNECTED) {
				return true;
			}
			
			//TODO: add support for more maps
			if (!radioMapOne.isChecked()) return true;
			
			final TextView tv = (TextView) view;
//			tv.setBackgroundColor(Color.GRAY);
			
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);

	    	// get edit_cell_dialog.xml view
	    	final LayoutInflater li = LayoutInflater.from(ctx);
	    	
	    	final View promptsView = li.inflate(R.layout.edit_cell_dialog, parent, false);
    		alertDialogBuilder.setView(promptsView);
	    	
	    	final CellValueWidget cellValueWidget = (CellValueWidget) promptsView.findViewById(R.id.cellValueWidget1);
			final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextNewValue);

			cellValueWidget.setEditText(userInput);
	    	
			userInput.setText(tv.getText());
//			userInput.selectAll();
			
			// set dialog message
			alertDialogBuilder
				.setCancelable(false)
				.setPositiveButton("OK",
				  new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog,int id) {
				    	
						short value = 0;
						
						final float tvv;
						try {
							tvv = Float.parseFloat(userInput.getText().toString());
						} catch (Exception ex) {
				    		AdaptiveLogger.log(Level.ERROR, "Bad user input from cellValueWidget - " + ex.getMessage());
							return;
						}
						
						// rewrite our adapter
						String[] temp = new String[fuelData.getCount()];
						
						for (int x = 0; x < fuelData.getCount() ; x++) {
							if (x == position) {
								temp[x] = String.format("%.2f", tvv);
							} else {
								temp[x] = fuelData.getItem(x);
							}
						}
						
						fuelData.clear();
						fuelData.addAll(temp);
						fuelData.notifyDataSetChanged();

						tv.setTextColor(Color.GREEN);
//						tv.setBackgroundColor(Color.BLACK);
						
						if ((radioMapOne.isChecked() && connectionService.isFuelMapOneVE()) || 
								(radioMapTwo.isChecked() && connectionService.isFuelMapTwoVE())) {
							// VE DIVISED
							value = (short) (tvv * VE_DIVISOR);
						} else {
							// MS DIVISED
							value = (short) (tvv * MS_DIVISOR);
						}
						
						// determine offset
						final int offset = position / 17 + 1;
						AdaptiveLogger.log("Fuel Table Position: " + (position - offset));
						
						//TODO: what about map 2 offset?
						connectionService.updateRegister((short) (position - offset), value);
				    }
				  })
				.setNegativeButton("Cancel",
				  new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog,int id) {
					dialog.cancel();
//					tv.setBackgroundColor(Color.TRANSPARENT);
				    }
				  });

			// create and show alert dialog
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
			
		    final int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
					   getScreenOrientation() == Configuration.ORIENTATION_PORTRAIT ? 380 : 240, 
					   getResources().getDisplayMetrics());

		    final int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
					  getScreenOrientation() == Configuration.ORIENTATION_PORTRAIT ? 240 : 380, 
					  getResources().getDisplayMetrics());

			WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		    lp.copyFrom(alertDialog.getWindow().getAttributes());
		    lp.width = width;
		    lp.height = height;
		    
		    alertDialog.getWindow().setAttributes(lp);
		    
			userInput.clearFocus();
			
			return true;
		}
		
		private int getScreenOrientation() {
		    Display getOrient = getWindowManager().getDefaultDisplay();
		    int orientation = Configuration.ORIENTATION_UNDEFINED;
		    if(getOrient.getWidth()==getOrient.getHeight()){
		        orientation = Configuration.ORIENTATION_SQUARE;
		    } else{ 
		        if(getOrient.getWidth() < getOrient.getHeight()){
		            orientation = Configuration.ORIENTATION_PORTRAIT;
		        }else { 
		             orientation = Configuration.ORIENTATION_LANDSCAPE;
		        }
		    }
		    return orientation;
		}
	};
	
	private void speakPhrase(final String phrase) {
		
		// bail if no tts
		if (!isTtsReady) return;
		
		final long millis = System.currentTimeMillis();
		
		// bail if in tts blackout
		if (millis - lastPhraseMillis < TTS_MIN_SILENCE_MILLIS) {
			return;
		}
		
		lastPhraseMillis = millis;
		
		// Request audio focus for playback
		int result = am.requestAudioFocus(afChangeListener,
		                                 // Use the music stream.
		                                 AudioManager.STREAM_MUSIC,
		                                 // Request permanent focus.
		                                 AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
		   
		if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            final HashMap<String, String> params = new HashMap<String, String>();
            params.put(Engine.KEY_PARAM_VOLUME, ".75");
            params.put(Engine.KEY_PARAM_UTTERANCE_ID, phrase);

    		speaker.speak(params.get(Engine.KEY_PARAM_UTTERANCE_ID), TextToSpeech.QUEUE_ADD, params); 
		}
	}
}

