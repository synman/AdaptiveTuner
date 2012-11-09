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
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
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

import com.shellware.adaptronic.adaptive.tuner.bluetooth.ConnectThread;
import com.shellware.adaptronic.adaptive.tuner.bluetooth.ConnectedThread;
import com.shellware.adaptronic.adaptive.tuner.changelog.ChangeLog;
import com.shellware.adaptronic.adaptive.tuner.gauges.GaugeNeedle;
import com.shellware.adaptronic.adaptive.tuner.modbus.ModbusRTU;
import com.shellware.adaptronic.adaptive.tuner.preferences.AdaptivePreferences;
import com.shellware.adaptronic.adaptive.tuner.usb.UsbConnectedThread;
import com.shellware.adaptronic.adaptive.tuner.valueobjects.LogItems;
import com.shellware.adaptronic.adaptive.tuner.valueobjects.LogItems.LogItem;

public class MainActivity extends Activity implements ActionBar.TabListener, OnClickListener {
	
	public static final String TAG = "Adaptive";
	public static final boolean DEBUG_MODE = false;
	
	public static final short CONNECTION_ERROR = 1;
	public static final short DATA_READY = 2;
	public static final short CONNECTED = 3;

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
	private ConnectionHandler connectionHandler = new ConnectionHandler();
	private static ConnectedThread connected;
	private static ConnectThread doConnect;
	
	private final BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();;
	
	private ArrayAdapter<String> devices;
	private static ArrayAdapter<String> dataArray;

	private static StringBuffer dataBuffer = new StringBuffer(512);
	
	private static float targetAFR = 0f;
	private static int lastRPM = 0;
	private static int tps = 0;
	private static boolean closedLoop = false;
	
	private static long updatesReceived = 0;
	private static long totalTimeMillis = 0;
	
	private static short lastRegister = 4096;
	private static long lastUpdateInMillis = 0;
	
	private static SharedPreferences prefs ;
	private static Context ctx;
	
	private static int tempUomPref = 1;
	private static boolean afrNotEqualTargetPref = false;
	private static float afrNotEqualTargetTolerance = 5f;
	private static boolean waterTempPref = false;
	private static float minimumWaterTemp = 0f;
	private static float maximumWaterTemp = 210f;
	private String remoteMacAddr = "";
	private String remoteName = "";
	private boolean autoConnect = false;
	
	private static boolean afrAlarmLogging = false;
	private final static LogItems afrAlarmLogItems = new LogItems();
	
//	private static TChart chart;
//	private static Surface ser;
	
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
        
//		LinearLayout group = (LinearLayout) findViewById(R.id.linearLayoutTchart);
//		chart = new TChart(this);
//		chart.setAutoRepaint(false);
//		
//		group.addView(chart);
//
//		ThemesList.applyTheme(chart.getChart(), 1);
//		
//		chart.removeAllSeries();
//		ser = new Surface(chart.getChart());
//	
//		chart.getAspect().setView3D(true);
//		ser.setIrregularGrid(true);
//		
//		ser.setUseColorRange(false);
//		ser.setUsePalette(true);
//		ser.setPaletteStyle(PaletteStyle.RAINBOW);
//		
//		chart.getPanel().setBorderRound(7);
//		
//		chart.getAspect().setView3D(true);
//		chart.getAspect().setChart3DPercent(100);
//		chart.getAspect().setOrthogonal(false);
//		chart.getAspect().setZoom(75);				
//		chart.getAspect().setRotation(350);
//		chart.getAspect().setElevation(350);
//		chart.getAspect().setPerspective(37);
//		
//		chart.getAxes().getDepth().setVisible(true);
//		chart.getLegend().setVisible(false);
//		chart.getHeader().setText("Fuel Map #1 (VE)");
//		chart.getHeader().getFont().setSize(18);
//		chart.getHeader().getFont().setBold(true);
//		
//		ser.setColor(com.steema.teechart.drawing.Color.darkGray);		
//		new Rotate(chart.getChart());
        
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
    }
    
    @Override
	protected void onResume() {
    	super.onResume();
    	
    	// initialize our preferences
    	tempUomPref = Integer.parseInt(prefs.getString("prefs_uom_temp", "1"));
    	afrNotEqualTargetPref = prefs.getBoolean("prefs_afrnottarget_pref", false);
    	afrNotEqualTargetTolerance = prefs.getFloat("prefs_afrnottarget_tolerance_pref", 5f);
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
 
//EVAN    	
        String action = getIntent().getAction();

        if (Intent.ACTION_MAIN.equals(action) || UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) { 
        	if (DEBUG_MODE) Log.d(TAG, "USB Device Attached");
        	        	
        	connected = UsbConnectedThread.checkConnectedUsbDevice(this, connectionHandler);
        }
//END EVAN    	
    	
    	if (connected != null && connected.isAlive()) {
    		refreshHandler.postDelayed(RefreshRunnable, LONG_PAUSE);
    	} else {
        	if (autoConnect && remoteMacAddr.length() > 0) {
        		disconnect();
        		new Handler().postDelayed(( new Runnable() { public void run() { connect(remoteName,  remoteMacAddr); } } ), 1000);
//        		connect(remoteName,  remoteMacAddr);
        	}
    	}
	}

    @Override
    protected void onPause() {
    	super.onPause();
    	refreshHandler.removeCallbacks(RefreshRunnable);
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		disconnect();
	}

	private final static Runnable RefreshRunnable = new Runnable() {

		public void run() {

        	// last time slice of data is trash -- discard it and try again
        	if (System.currentTimeMillis() - lastUpdateInMillis > LONG_PAUSE) {
        		if (DEBUG_MODE) Log.d(TAG, lastRegister + " response timed out: " + dataBuffer.toString());

        		sendRequest();
				
				imgStatus.setBackgroundColor(Color.RED);				
        	}
        	
    		refreshHandler.postDelayed(this, LONG_PAUSE);
        }
    };

	private static class ConnectionHandler extends Handler {

		@Override
		public void handleMessage(Message message) {
		
	        switch (message.getData().getShort("handle")) {
	        	case CONNECTED: 
	    	        if (progress != null && progress.isShowing()) progress.dismiss();
		    		menuConnect.setTitle(R.string.menu_disconnect);
		    		sendRequest(REGISTER_4096_PLUS_SEVEN);
		    		totalTimeMillis = 0;
		    		updatesReceived = 0;

		    		refreshHandler.postDelayed(RefreshRunnable, LONG_PAUSE);

	        		Editor edit = prefs.edit();
	        		edit.putString("prefs_remote_name", message.getData().getString("name"));
	        		edit.putString("prefs_remote_mac", message.getData().getString("addr"));
	        		edit.commit();
		    		
		    		break;
		    		
	        	case CONNECTION_ERROR:
	    	        if (progress != null && progress.isShowing()) progress.dismiss();

					disconnect();

//					AlertDialog alert = new AlertDialog.Builder(ctx).create();
//					alert.setTitle(message.getData().getString("title"));
//					alert.setMessage("\n" + message.getData().getString("message") + "\n");
//					alert.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", 
//							new DialogInterface.OnClickListener() {	
//								public void onClick(DialogInterface dialog, int which) {
//									dialog.dismiss();
//								}
//							});
//					alert.show();
					
					Toast.makeText(ctx, message.getData().getString("message"), Toast.LENGTH_LONG).show();

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
	    	        		
	    	        		if (mapMode) {
    	        				progress.setMessage(String.format("Reading map values %d/512...", mapOffset));
	    	        			
	    	        			if (data.contains(SIXTEEN_REGISTERS) && datalength >= 37 && ModbusRTU.validCRC(data.trim().split(" "), 37)) {
	    	        				populateAndPlotFuelMap(data);
	    	        			}
	    	        			
	    	        			break;
	    	        		}
	    	        		
	    	        		// do we have a bad message?
	    	        		if (!(data.contains(EIGHT_REGISTERS) || data.contains(SEVEN_REGISTERS))) {
	    	            		if (DEBUG_MODE) Log.d(TAG, lastRegister + " response discarded: " + data);
	    	    				imgStatus.setBackgroundColor(Color.RED);				
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
    
		if (connected != null & connected.isAlive()) {
			connected.write(ModbusRTU.getRegister(SLAVE_ADDRESS, HOLDING_REGISTER, register, length));
			
	    	lastUpdateInMillis = currentTimeInMillis;
	    	lastRegister = register;
		}
	}
	
	private static void populateAndPlotFuelMap(final String data) {
		
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

		String[] map = mapData.toString().trim().split(" ");
		short cnt = 0;
		
//		chart.setAutoRepaint(false);
//		ser.clear();
//	
//		for (int x = 0; x < 32; x++) {
//			for (int y = 0; y < 16; y++) {
//				double val = Double.parseDouble(String.format("%.0f", Integer.parseInt(map[cnt] + map[cnt+1], 16) / 128f));
//				val = val - 95;
//				ser.add(x, val, y);
//				if (DEBUG_MODE) Log.d(TAG, String.format("%d:%d = %.0f", x, y, val));
//				cnt = (short) (cnt + 2);
//			}
//		}	  
//		
//		for (int x = 0; x < chart.getAxes().getCount(); x++) {
//			chart.getAxes().getAxis(x).getLabels().getItems().clear();
//		}
//		
//		for (double x = 0; x < 32; x++) {
//			AxisLabelItem itm = chart.getAxes().getBottom().getLabels().getItems().add(x);
//			itm.getFont().setSize(10);
//			itm.getFont().setColor(com.steema.teechart.drawing.Color.white);
//			itm.setText(String.format("%.0f", 300 * x));
//		}
//		
//		for (double x = 0; x < 16; x++) {
//			AxisLabelItem itm = chart.getAxes().getDepth().getLabels().getItems().add(x, x == 0 ? " " : String.format("%.0f", 13 * x));
//			itm.getFont().setSize(10);
//			itm.getFont().setColor(com.steema.teechart.drawing.Color.white);
//		}
//
//		chart.getAxes().getBottom().getTitle().setText("Engine RPM");
//		chart.getAxes().getBottom().getTitle().getFont().setColor(com.steema.teechart.drawing.Color.white);
//		chart.getAxes().getBottom().getTitle().getFont().setSize(14);
//
//		chart.getAxes().getDepth().getTitle().setText("MAP (kPa)");
//		chart.getAxes().getDepth().getTitle().getFont().setColor(com.steema.teechart.drawing.Color.white);
//		chart.getAxes().getDepth().getTitle().getFont().setSize(14);
//		
//		chart.refreshControl();
//		chart.setAutoRepaint(true);
		
        if (progress != null && progress.isShowing()) progress.dismiss();
		
		lastRegister = REGISTER_4096_PLUS_SEVEN;		
		mapMode = false;		
	}
	
	private static void process4140Response(final String data) {
		    	
		final String[] buf = data.substring(data.indexOf(SEVEN_REGISTERS), data.length()).split(" ");

		if (ModbusRTU.validCRC(buf, REGISTER_4140_LENGTH)) {   
			imgStatus.setBackgroundColor(Color.GREEN);

			setLearningFlags(new String[] {"0", "0", "0", buf[15], buf[16]});
			targetAFR = Integer.parseInt(buf[3] + buf[4], 16) / 10f;
			closedLoop = (getBit(Integer.parseInt(buf[9] + buf[10], 16), 8) > 0); 
			
			if (closedLoop) {
				txtFuelLearn.setBackgroundColor(Color.GREEN);
			} else {
				txtFuelLearn.setBackgroundColor(Color.TRANSPARENT);
			}

			txtData.setText(String.format("AVG: %d ms - TPS: %d%%", (int) totalTimeMillis / updatesReceived, tps));
			
            if (DEBUG_MODE) Log.d(TAG, "Processed " + lastRegister + " response: " + data);
			sendRequest(REGISTER_4096_PLUS_SEVEN);            
            return;
            
		} else {
			if (DEBUG_MODE) Log.d(TAG, "bad CRC for " + lastRegister + ": " + data);
			imgStatus.setBackgroundColor(Color.RED);
			sendRequest();
		}
	}
	
    private static void process4096Response(final String data) {
    	
		final String[] buf = data.substring(data.indexOf(EIGHT_REGISTERS), data.length()).split(" ");

		if (ModbusRTU.validCRC(buf, REGISTER_4096_LENGTH)) {   
			imgStatus.setBackgroundColor(Color.GREEN);
    		dataArray.clear();
    		
    		final int rpm = Integer.parseInt(buf[3] + buf[4], 16);
    		final int map = Integer.parseInt(buf[5] + buf[6], 16);
    		final int mat = getTemperatureValue(buf[7] + buf[8]);
    		final int wat = getTemperatureValue(buf[9] + buf[10]);
    		
    		final float afr = Integer.parseInt(buf[14], 16) / 10f;
    		final float referenceAfr = Integer.parseInt(buf[13], 16) / 10f;
    		
    		tps = Integer.parseInt(buf[17] + buf[18], 16);
    		
    		iatNeedle.setValue(mat);
    		waterNeedle.setValue(Integer.parseInt(buf[9] + buf[10], 16) * 9 / 5 + 32);
    		mapNeedle.setValue(map);
    		
    		{
        		float afrVal = afr * 100;
        		float targetAfrVal = targetAFR * 100;
        		
        		if (afrVal > AFR_MAX) afrVal = AFR_MAX;
        		if (afrVal < AFR_MIN) afrVal = AFR_MIN;
        		
        		if (targetAfrVal > AFR_MAX) targetAfrVal = AFR_MAX;
        		if (targetAfrVal < AFR_MIN) targetAfrVal = AFR_MIN;

        		afrNeedle.setValue(AFR_MAX - afrVal + AFR_MIN);
        		targetAfrNeedle.setValue(AFR_MAX - targetAfrVal + AFR_MIN);
    		}
    		
    		if (rpm >= 200) lastRPM = rpm;
    		dataArray.add(String.format("RPM\n%d", lastRPM));
    		rpmNeedle.setValue(lastRPM);
    		
    		dataArray.add(String.format("MAP\n%d kPa", map));
    		dataArray.add(String.format("MAT\n%d\u00B0 %s", mat, getTemperatureSymbol()));
    		dataArray.add(String.format("AFR\n%.1f (%.1f)", afr, referenceAfr));
    		dataArray.add("TAFR\n" +  (targetAFR != 0f ? String.format("%.1f", targetAFR) : "--.-"));
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
        			final float threshold = targetAFR * (afrNotEqualTargetTolerance * .01f);
        			if (Math.abs(targetAFR - afr) >= threshold ) {
        				if (afr > targetAFR) {
        					gridData.getChildAt(3).setBackgroundColor(Color.RED);
        				} else {
        					gridData.getChildAt(3).setBackgroundColor(Color.BLUE);
        				}
        				
        				if (afrAlarmLogging) {
        					LogItem newItem = afrAlarmLogItems.newLogItem();
        					newItem.setAfr(afr);
        					newItem.setReferenceAfr(referenceAfr);
        					newItem.setMap(map);
        					newItem.setMat(mat);
        					newItem.setRpm(rpm);
        					newItem.setTargetAfr(targetAFR);
        					newItem.setWat(wat);
        					newItem.setTps(tps);
        					
        					afrAlarmLogItems.getItems().add(newItem);
        					
        					if (!menuShareLog.isVisible()) {
        						menuShareLog.setVisible(true);
        					}
        				}
        			}
        		}
    		}
    		
            if (DEBUG_MODE) Log.d(TAG, "Processed " + lastRegister + " response: " + data);
    		sendRequest(REGISTER_4140_PLUS_SIX);
            return;
            
		} else {
			if (DEBUG_MODE) Log.d(TAG, "bad CRC for " + lastRegister + ": " + data);
			imgStatus.setBackgroundColor(Color.RED);
    		sendRequest();
		}
    }
    
	private static void setLearningFlags(String[] buf) {
		
		imgFWait.setBackgroundColor(Color.TRANSPARENT);
		imgFRpm.setBackgroundColor(Color.TRANSPARENT);
		imgFLoad.setBackgroundColor(Color.TRANSPARENT);
		
		imgIWait.setBackgroundColor(Color.TRANSPARENT);
		imgIRpm.setBackgroundColor(Color.TRANSPARENT);
		imgILoad.setBackgroundColor(Color.TRANSPARENT);
		
		 if (getBit(Integer.parseInt(buf[3] + buf[4], 16), 0) > 0) 
			 imgFWait.setBackgroundColor(Color.parseColor("#FFCC00"));
		 if (getBit(Integer.parseInt(buf[3] + buf[4], 16), 1) > 0) 
			 imgFRpm.setBackgroundColor(Color.GREEN);
		 if (getBit(Integer.parseInt(buf[3] + buf[4], 16), 2) > 0) 
			 imgFLoad.setBackgroundColor(Color.GREEN);

		 if (getBit(Integer.parseInt(buf[3] + buf[4], 16), 3) > 0) 
			 imgIWait.setBackgroundColor(Color.parseColor("#FFCC00"));
		 if (getBit(Integer.parseInt(buf[3] + buf[4], 16), 4) > 0) 
			 imgIRpm.setBackgroundColor(Color.GREEN);
		 if (getBit(Integer.parseInt(buf[3] + buf[4], 16), 5) > 0) 
			imgILoad.setBackgroundColor(Color.GREEN);
	}
	
    private static int getBit(final int item, final int position) {   
    	return (item >> position) & 1;
    }
    
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
    
    private static void clearDataBuffer() {
    	synchronized(ctx) {
			dataBuffer.setLength(0);
    	}
    }

    private final static StringBuffer setDataBuffer(final byte[] data, final int length) {
    	synchronized(ctx) {
	        for (int x = 0; x < length; x++) {
	        	dataBuffer.append(String.format("%02X ", data[x]));
	        }
        	return dataBuffer;
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
    	
    	progress = ProgressDialog.show(this, "Bluetooth Connection" , "Connecting to " + name);
    	progress.setCancelable(true);
    	progress.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface arg0) {
				if (doConnect != null && doConnect.isAlive() ) {
					doConnect.cancel();
					try {
						doConnect.join();
					} catch (InterruptedException e) {
						// do nothing
					}
				}
			}});
    	
    	connected = new ConnectedThread(connectionHandler);    	
    	doConnect = new ConnectThread(connectionHandler, name, macAddr, connected);
    	doConnect.start();
    }
    
    private static void disconnect() {
    	
    	refreshHandler.removeCallbacks(RefreshRunnable);
		if (menuConnect != null) menuConnect.setTitle(R.string.menu_connect);
		imgStatus.setBackgroundColor(Color.TRANSPARENT);				
    	
		try {
	    	if (connected != null && connected.isAlive()) {
	    		connected.cancel();
	    		connected.join();
	    	}
		} catch (Exception e) {
			// do nothing
		}	
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
	        	System.exit(0);
	        	
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
					f.write("timestamp, rpm, map, targetafr, afr, refafr, tps, wat, mat\n".getBytes());
					
					ArrayList<LogItem> items = afrAlarmLogItems.getItems();
					Iterator<LogItem> iterator = items.iterator();
					
					while (iterator.hasNext()) {
						final LogItem item = (LogItem) iterator.next();
						f.write(item.getLogBytes());
					}
					
					afrAlarmLogItems.getItems().clear();
					
					f.flush();
					f.close();
					
					menuShareLog.setVisible(false);
					
					Toast.makeText(getApplicationContext(), String.format("Log saved as %s%s%s", sdcard.getAbsolutePath(), "/AdaptiveTuner/", filename), Toast.LENGTH_LONG).show();
					if (DEBUG_MODE) Log.d(TAG, String.format("Log saved as %s%s%s", sdcard.getAbsolutePath(), "/AdaptiveTuner/", filename));

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
				if (connected != null && connected.isAlive()) {
			    	progress = ProgressDialog.show(ctx, "Fuel Map" , "Reading map values 0/512...");

			    	mapOffset = 0;
			    	mapMode = true;
			    	sendRequest(mapOffset);
				}
				
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
