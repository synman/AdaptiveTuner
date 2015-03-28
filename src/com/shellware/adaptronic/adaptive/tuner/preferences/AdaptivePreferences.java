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
package com.shellware.adaptronic.adaptive.tuner.preferences;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;

import com.shellware.adaptronic.adaptive.tuner.R;
import com.shellware.adaptronic.adaptive.tuner.logging.AdaptiveLogger;

public class AdaptivePreferences extends PreferenceActivity {

	private static SharedPreferences prefs ;
	private static Editor edit;
		
	private static AdaptivePreferences ctx;
	private static Resources res;
	
    // Local Bluetooth adapter
//    private static BluetoothAdapter bta = null;

    public static final float MIN_WATER_TEMP_CELSIUS = 72f;
    public static final float MAX_WATER_TEMP_CELSIUS = 98f;
    
    public static final float MIN_WATER_TEMP_FAHRENHEIT = 160f;
    public static final float MAX_WATER_TEMP_FAHRENHEIT = 210f;

    @SuppressLint("CommitPrefEdits")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
//    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    	super.onCreate(savedInstanceState);
        
        AdaptiveLogger.log("Preferences onCreate");

        // Get local Bluetooth adapter
//        bta = BluetoothAdapter.getDefaultAdapter();
	    
        ctx = this;
        res = getResources();

    	prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	edit = prefs.edit();

        setTitle(R.string.prefs_title);
		getActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		
        // Handle item selection
        switch (item.getItemId()) {
	        case android.R.id.home:
	        	this.finish();
        }
        return true;
	}
	
    /* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
	    AdaptiveLogger.log("Preferences onResume");
	}

	public static class GeneralFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceScreen generalPref = getPreferenceManager().createPreferenceScreen(ctx);
            generalPref.setKey("prefs_general");
            generalPref.setTitle(R.string.prefs_general_pref);
            
	        	PreferenceCategory uomPrefCat = new PreferenceCategory(ctx);
	        	uomPrefCat.setTitle(R.string.prefs_uom_category);
	        	generalPref.addPreference(uomPrefCat);
		        
			        // List preference
			        final ListPreference uomTempPref = new ListPreference(ctx);
			        uomTempPref.setPersistent(true);
			        uomTempPref.setKey("prefs_uom_temp");
			        uomTempPref.setEntries(R.array.temperature_uom_names);
			        uomTempPref.setEntryValues(R.array.temperature_uom_values);
			        uomTempPref.setDialogTitle(R.string.prefs_uom_temp_dialog_title);
			        uomTempPref.setTitle(R.string.prefs_uom_temp_title);
			        uomTempPref.setDefaultValue("1");
			        uomTempPref.setSummary(res.getStringArray(R.array.temperature_uom_names) 
			        		[Integer.parseInt(prefs.getString("prefs_uom_temp", "1"))]);
				        
			        uomTempPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						public boolean onPreferenceChange(Preference arg0, final Object arg1) {
					        uomTempPref.setSummary(res.getStringArray(R.array.temperature_uom_names) 
					        		[Integer.parseInt((String) arg1)]);	
					        
					        // reset our water temp alarms as they are no longer valid
					        switch (Integer.parseInt((String) arg1)) {
						        case 0:
							        edit.putFloat("prefs_min_water_temp", MIN_WATER_TEMP_CELSIUS);
							        edit.putFloat("prefs_max_water_temp", MAX_WATER_TEMP_CELSIUS);
							        break;
						        case 1:
							        edit.putFloat("prefs_min_water_temp", MIN_WATER_TEMP_FAHRENHEIT);
							        edit.putFloat("prefs_max_water_temp", MAX_WATER_TEMP_FAHRENHEIT);
					        }
					        
					        edit.commit();
							return true;
						}
			        });
			        
		        uomPrefCat.addPreference(uomTempPref);
			        
	        	PreferenceCategory connectionPrefCat = new PreferenceCategory(ctx);
	        	connectionPrefCat.setTitle(R.string.prefs_connection_category);
	        	generalPref.addPreference(connectionPrefCat);
		        
			        CheckBoxPreference autoConnectPref = new CheckBoxPreference(ctx);
			        autoConnectPref.setPersistent(true);
			        autoConnectPref.setKey("prefs_auto_connect");
			        autoConnectPref.setDefaultValue(false);
			        autoConnectPref.setSummaryOn(R.string.enabled);
			        autoConnectPref.setSummaryOff(R.string.disabled);
			        autoConnectPref.setTitle(R.string.prefs_connection_auto_connect);
				        
		        connectionPrefCat.addPreference(autoConnectPref);
		        
			        CheckBoxPreference connectChargePref = new CheckBoxPreference(ctx);
			        connectChargePref.setPersistent(true);
			        connectChargePref.setKey("prefs_connect_on_charge");
			        connectChargePref.setDefaultValue(false);
			        connectChargePref.setSummaryOn(R.string.enabled);
			        connectChargePref.setSummaryOff(R.string.disabled);
			        connectChargePref.setTitle(R.string.prefs_connect_on_charge);
				        
		        connectionPrefCat.addPreference(connectChargePref);
		        
			        final SeekBarPreference connectChargeWaitPref = new SeekBarPreference(ctx);
			        
			        connectChargeWaitPref.setPersistent(false);
			        connectChargeWaitPref.setTitle(R.string.prefs_connect_on_charge_wait_time);
			        connectChargeWaitPref.setSuffix(" seconds");
			        connectChargeWaitPref.setMinValue(1f);
			        connectChargeWaitPref.setMaxValue(60);
			        connectChargeWaitPref.setScale(1f);
					connectChargeWaitPref.setSummary(String.format("%.0f seconds", prefs.getFloat("prefs_connect_on_charge_wait_time", 30f)));
			        connectChargeWaitPref.setDefaultValue(prefs.getFloat("prefs_connect_on_charge_wait_time", 5f));
			        connectChargeWaitPref.setEnabled(prefs.getBoolean("prefs_connect_on_charge", false));
			        
			        connectChargeWaitPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						public boolean onPreferenceChange(Preference arg0, Object arg1) {
							connectChargeWaitPref.setSummary(String.format("%.0f seconds", (Float) arg1));
							edit.putFloat("prefs_connect_on_charge_wait_time", (Float) arg1);
							edit.commit();
							return true;
						}
			        });	
			        
			        connectChargePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						public boolean onPreferenceChange(Preference arg0, Object arg1) {
							connectChargeWaitPref.setEnabled((Boolean) arg1);
							return true;
						}
			        });
			        
		       connectionPrefCat.addPreference(connectChargeWaitPref);
		        
		        	CheckBoxPreference wakeLockPref = new CheckBoxPreference(ctx);
			        wakeLockPref.setPersistent(true);
			        wakeLockPref.setKey("prefs_wake_lock");
			        wakeLockPref.setDefaultValue(true);
			        wakeLockPref.setSummaryOn(R.string.enabled);
			        wakeLockPref.setSummaryOff(R.string.disabled);
			        wakeLockPref.setTitle(R.string.prefs_connection_keep_device_awake);
				        
		        connectionPrefCat.addPreference(wakeLockPref);		       
		 
        	PreferenceCategory displayPrefCat = new PreferenceCategory(ctx);
        	displayPrefCat.setTitle(R.string.prefs_display_category);
        	generalPref.addPreference(displayPrefCat);
	        
		        CheckBoxPreference showAuxTPref = new CheckBoxPreference(ctx);
		        showAuxTPref.setPersistent(true);
		        showAuxTPref.setKey("prefs_show_auxt");
		        showAuxTPref.setDefaultValue(false);
		        showAuxTPref.setSummaryOn(R.string.enabled);
		        showAuxTPref.setSummaryOff(R.string.disabled);
		        showAuxTPref.setTitle(R.string.prefs_show_auxt_on_adaptive_tab);
			        
		        displayPrefCat.addPreference(showAuxTPref);
	
		        final CheckBoxPreference showBoostPref = new CheckBoxPreference(ctx);
		        showBoostPref.setPersistent(true);
		        showBoostPref.setKey("prefs_show_boost");
		        showBoostPref.setDefaultValue(false);
		        showBoostPref.setSummaryOn(R.string.enabled);
		        showBoostPref.setSummaryOff(R.string.disabled);
		        showBoostPref.setTitle(R.string.prefs_show_boost_gauge);
		        
		        displayPrefCat.addPreference(showBoostPref);
		        
		        // List preference
		        final ListPreference maxBoostPref = new ListPreference(ctx);
		        maxBoostPref.setEnabled(showBoostPref.isChecked());
		        maxBoostPref.setPersistent(true);
		        maxBoostPref.setKey("prefs_max_boost");
		        maxBoostPref.setEntries(R.array.max_boost_names);
		        maxBoostPref.setEntryValues(R.array.max_boost_values);
		        maxBoostPref.setDialogTitle(R.string.prefs_max_boost);
		        maxBoostPref.setTitle(R.string.prefs_max_boost);
		        maxBoostPref.setDefaultValue("0");
		        maxBoostPref.setSummary(res.getStringArray(R.array.max_boost_names) 
		        		[Integer.parseInt(prefs.getString("prefs_max_boost", "0"))]);
			        
		        maxBoostPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference arg0, final Object arg1) {
				        maxBoostPref.setSummary(res.getStringArray(R.array.max_boost_names) 
				        		[Integer.parseInt((String) arg1)]);	
						return true;
					}
		        });
		        
	        displayPrefCat.addPreference(maxBoostPref);

		        showBoostPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference arg0, final Object arg1) {
						maxBoostPref.setEnabled((Boolean) arg1);
						return true;
					}        	
		        });
		        	
	        setPreferenceScreen(generalPref);
        }
    }
    
    public static class AlertsFragment extends PreferenceFragment {
    	
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceScreen alertsPref = getPreferenceManager().createPreferenceScreen(ctx);
            alertsPref.setKey("prefs_alerts");
            alertsPref.setTitle(R.string.prefs_alerts_pref);
            
	        	PreferenceCategory audiblesPrefCat = new PreferenceCategory(ctx);
	        	audiblesPrefCat.setTitle(R.string.prefs_alerts_audibles);
	        	audiblesPrefCat.setKey(audiblesPrefCat.getTitle().toString());
	        	alertsPref.addPreference(audiblesPrefCat);
	        	
	        	
			        CheckBoxPreference audiblesPref = new CheckBoxPreference(ctx);
			        audiblesPref.setPersistent(true);
			        audiblesPref.setKey("prefs_audibles_pref");
			        audiblesPref.setDefaultValue(false);
			        audiblesPref.setSummaryOn(R.string.enabled);
			        audiblesPref.setSummaryOff(R.string.disabled);
			        audiblesPref.setTitle(R.string.prefs_alerts_audible_alerts);
			        
			        audiblesPrefCat.addPreference(audiblesPref);
			        
			        final SeekBarPreference audiblesMaxMap = new SeekBarPreference(ctx);
			        
			        audiblesMaxMap.setPersistent(false);
			        audiblesMaxMap.setTitle(R.string.prefs_audibles_max_map);
			        audiblesMaxMap.setSummary(String.format("%d KPA", prefs.getInt("prefs_audibles_max_map", 150)));
			        audiblesMaxMap.setSuffix(" KPA");
			        audiblesMaxMap.setMinValue(0f);
			        audiblesMaxMap.setMaxValue(200);
			        audiblesMaxMap.setScale(5f);
			        audiblesMaxMap.setDefaultValue((float) prefs.getInt("prefs_audibles_max_map", 150));
			        audiblesMaxMap.setEnabled(prefs.getBoolean("prefs_audibles_pref", false));
			        
			        audiblesMaxMap.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						public boolean onPreferenceChange(Preference arg0, Object arg1) {
							final int val = Math.round((Float) arg1);
							audiblesMaxMap.setSummary(String.format("%d KPA", val));
							edit.putInt("prefs_audibles_max_map", val);
							edit.commit();
							return true;
						}
			        });
			        
			        audiblesPrefCat.addPreference(audiblesMaxMap);	
			        
			        final SeekBarPreference audiblesMaxMat = new SeekBarPreference(ctx);
			        
			        audiblesMaxMat.setPersistent(false);
			        audiblesMaxMat.setTitle(R.string.prefs_audibles_max_mat);
			        audiblesMaxMat.setSummary(String.format("%d\u00B0", prefs.getInt("prefs_audibles_max_mat", 120)));
			        audiblesMaxMat.setSuffix("\u00B0");
			        audiblesMaxMat.setMinValue(50);
			        audiblesMaxMat.setMaxValue(200);
			        audiblesMaxMat.setScale(5f);
			        audiblesMaxMat.setDefaultValue((float) prefs.getInt("prefs_audibles_max_mat", 120));
			        audiblesMaxMat.setEnabled(prefs.getBoolean("prefs_audibles_pref", false));
			        
			        audiblesMaxMat.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						public boolean onPreferenceChange(Preference arg0, Object arg1) {
							final int val = Math.round((Float) arg1);
							audiblesMaxMat.setSummary(String.format("%d\u00B0", val));
							edit.putInt("prefs_audibles_max_mat", val);
							edit.commit();
							return true;
						}
			        });
			        
			        audiblesPrefCat.addPreference(audiblesMaxMat);			        
			        audiblesPrefCat.addPreference(audiblesMaxMap);	
			        
			        final SeekBarPreference audiblesMaxRpm = new SeekBarPreference(ctx);
			        
			        audiblesMaxRpm.setPersistent(false);
			        audiblesMaxRpm.setTitle(R.string.prefs_audibles_max_rpm);
			        audiblesMaxRpm.setSummary(String.format("%d RPM", prefs.getInt("prefs_audibles_max_rpm", 7000)));
			        audiblesMaxRpm.setSuffix(" RPM");
			        audiblesMaxRpm.setMinValue(5000);
			        audiblesMaxRpm.setMaxValue(10000);
			        audiblesMaxRpm.setScale(250f);
			        audiblesMaxRpm.setDefaultValue((float) prefs.getInt("prefs_audibles_max_rpm", 7000));
			        audiblesMaxRpm.setEnabled(prefs.getBoolean("prefs_audibles_pref", false));
			        
			        audiblesMaxRpm.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						public boolean onPreferenceChange(Preference arg0, Object arg1) {
							final int val = Math.round((Float) arg1);
							audiblesMaxRpm.setSummary(String.format("%d RPM", val));
							edit.putInt("prefs_audibles_max_rpm", val);
							edit.commit();
							return true;
						}
			        });
			        
			        audiblesPrefCat.addPreference(audiblesMaxRpm);
			        
			        final SeekBarPreference audiblesMaxTps = new SeekBarPreference(ctx);
			        
			        audiblesMaxTps.setPersistent(false);
			        audiblesMaxTps.setTitle(R.string.prefs_audibles_max_tps);
			        audiblesMaxTps.setSummary(String.format("%d%%", prefs.getInt("prefs_audibles_max_tps", 80)));
			        audiblesMaxTps.setSuffix("%");
			        audiblesMaxTps.setMinValue(0);
			        audiblesMaxTps.setMaxValue(100);
			        audiblesMaxTps.setScale(5f);
			        audiblesMaxTps.setDefaultValue((float) prefs.getInt("prefs_audibles_max_tps", 80));
			        audiblesMaxTps.setEnabled(prefs.getBoolean("prefs_audibles_pref", false));
			        
			        audiblesMaxTps.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						public boolean onPreferenceChange(Preference arg0, Object arg1) {
							final int val = Math.round((Float) arg1);
							audiblesMaxTps.setSummary(String.format("%d%%", val));
							edit.putInt("prefs_audibles_max_tps", val);
							edit.commit();
							return true;
						}
			        });
			        
			        audiblesPrefCat.addPreference(audiblesMaxTps);			        
			        final SeekBarPreference audiblesMaxWat = new SeekBarPreference(ctx);
			        
			        audiblesMaxWat.setPersistent(false);
			        audiblesMaxWat.setTitle(R.string.prefs_audibles_max_wat);
			        audiblesMaxWat.setSummary(String.format("%d\u00B0", prefs.getInt("prefs_audibles_max_wat", 200)));
			        audiblesMaxWat.setSuffix("\u00B0");
			        audiblesMaxWat.setMinValue(50);
			        audiblesMaxWat.setMaxValue(240);
			        audiblesMaxWat.setScale(1f);
			        audiblesMaxWat.setDefaultValue((float) prefs.getInt("prefs_audibles_max_wat", 200));
			        audiblesMaxWat.setEnabled(prefs.getBoolean("prefs_audibles_pref", false));
			        
			        audiblesMaxWat.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						public boolean onPreferenceChange(Preference arg0, Object arg1) {
							final int val = Math.round((Float) arg1);
							audiblesMaxWat.setSummary(String.format("%d\u00B0", val));
							edit.putInt("prefs_audibles_max_wat", val);
							edit.commit();
							return true;
						}
			        });
			        
			        audiblesPrefCat.addPreference(audiblesMaxWat);
			        
			        audiblesPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						public boolean onPreferenceChange(Preference arg0, Object arg1) {
							final boolean enabled = (Boolean) arg1;
							audiblesMaxMap.setEnabled(enabled);
							audiblesMaxMat.setEnabled(enabled);
							audiblesMaxRpm.setEnabled(enabled);
							audiblesMaxTps.setEnabled(enabled);
							audiblesMaxWat.setEnabled(enabled);
							return true;
						}
			        });
			        
            	PreferenceCategory afrPrefCat = new PreferenceCategory(ctx);
            	afrPrefCat.setTitle(R.string.prefs_alert_afr_category);
            	alertsPref.addPreference(afrPrefCat);
	        
			        CheckBoxPreference afrNotTargetPref = new CheckBoxPreference(ctx);
			        afrNotTargetPref.setPersistent(true);
			        afrNotTargetPref.setKey("prefs_afrnottarget_pref");
			        afrNotTargetPref.setDefaultValue(false);
			        afrNotTargetPref.setSummaryOn(R.string.enabled);
			        afrNotTargetPref.setSummaryOff(R.string.disabled);
			        afrNotTargetPref.setTitle(R.string.prefs_alert_afrnottarget);
			        
			        final SeekBarPreference afrNotTargetTolPref = new SeekBarPreference(ctx);
			        
			        afrNotTargetTolPref.setPersistent(false);
			        afrNotTargetTolPref.setTitle(R.string.prefs_alert_afrnottarget_tolerance_pref);
			        afrNotTargetTolPref.setSummary(String.format("%.2f%%", prefs.getFloat("prefs_afrnottarget_tolerance_pref", 5f)));
			        afrNotTargetTolPref.setSuffix("%");
			        afrNotTargetTolPref.setMinValue(0f);
			        afrNotTargetTolPref.setMaxValue(10);
			        afrNotTargetTolPref.setScale(.25f);
			        afrNotTargetTolPref.setDefaultValue(prefs.getFloat("prefs_afrnottarget_tolerance_pref", 5f));
			        afrNotTargetTolPref.setEnabled(prefs.getBoolean("prefs_afrnottarget_pref", false));
			        
			        afrNotTargetTolPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						public boolean onPreferenceChange(Preference arg0, Object arg1) {
							afrNotTargetTolPref.setSummary(String.format("%.2f%%", (Float) arg1));
							edit.putFloat("prefs_afrnottarget_tolerance_pref", (Float) arg1);
							edit.commit();
							return true;
						}
			        });
	
			        final CheckBoxPreference afrLoggingPref = new CheckBoxPreference(ctx);
			        
			        afrLoggingPref.setPersistent(true);
			        afrLoggingPref.setKey("prefs_afr_alarm_logging");
			        afrLoggingPref.setDefaultValue(false);
			        afrLoggingPref.setSummaryOn(R.string.enabled);
			        afrLoggingPref.setSummaryOff(R.string.disabled);
			        afrLoggingPref.setTitle(R.string.prefs_afr_alarm_logging);
			        afrLoggingPref.setEnabled(prefs.getBoolean("prefs_afrnottarget_pref", false));
			        
			        afrNotTargetPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						public boolean onPreferenceChange(Preference arg0, Object arg1) {
							afrNotTargetTolPref.setEnabled((Boolean) arg1);
							afrLoggingPref.setEnabled((Boolean) arg1);
							return true;
						}
			        });
		         
			        afrPrefCat.addPreference(afrNotTargetPref);
			        afrPrefCat.addPreference(afrNotTargetTolPref);
			        afrPrefCat.addPreference(afrLoggingPref);
		        
            	PreferenceCategory tempPrefCat = new PreferenceCategory(ctx);
            	tempPrefCat.setTitle(R.string.prefs_alert_temperature_category);
            	alertsPref.addPreference(tempPrefCat);

			        //prefs_alert_water_temperature
			        CheckBoxPreference waterTempPref = new CheckBoxPreference(ctx);
			        waterTempPref.setPersistent(true);
			        waterTempPref.setKey("prefs_watertemp_pref");
			        waterTempPref.setDefaultValue(false);
			        waterTempPref.setSummaryOn(R.string.enabled);
			        waterTempPref.setSummaryOff(R.string.disabled);
			        waterTempPref.setTitle(R.string.prefs_alert_water_temperature);
			        
			        final SeekBarPreference minWaterTempPref = new SeekBarPreference(ctx);
			        
			        minWaterTempPref.setPersistent(false);
			        minWaterTempPref.setTitle(R.string.prefs_alert_water_temperature_minimum);
			        minWaterTempPref.setSuffix("\u00B0");
			        minWaterTempPref.setEnabled(prefs.getBoolean("prefs_watertemp_pref", false));
			        
			        minWaterTempPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						public boolean onPreferenceChange(Preference arg0, Object arg1) {
							minWaterTempPref.setSummary(String.format("%.0f\u00B0", (Float) arg1));
							edit.putFloat("prefs_min_water_temp", (Float) arg1);
							edit.commit();
							return true;
						}
			        });		
	
			        final SeekBarPreference maxWaterTempPref = new SeekBarPreference(ctx);
	
			        maxWaterTempPref.setPersistent(false);
			        maxWaterTempPref.setTitle(R.string.prefs_alert_water_temperature_maximum);
			        maxWaterTempPref.setSuffix("\u00B0");
			        maxWaterTempPref.setEnabled(prefs.getBoolean("prefs_watertemp_pref", false));
			        
			        maxWaterTempPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						public boolean onPreferenceChange(Preference arg0, Object arg1) {
							maxWaterTempPref.setSummary(String.format("%.0f\u00B0", (Float) arg1));
							edit.putFloat("prefs_max_water_temp", (Float) arg1);
							edit.commit();
							return true;
						}
			        });		
	
			        waterTempPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						public boolean onPreferenceChange(Preference arg0, Object arg1) {
							final boolean enabled = (Boolean) arg1;
							minWaterTempPref.setEnabled(enabled);
							maxWaterTempPref.setEnabled(enabled);
							return true;
						}
			        });
			        
			        switch (Integer.parseInt(prefs.getString("prefs_uom_temp", "1"))) {
			        	case 0:  // celsius
					        minWaterTempPref.setSummary(String.format("%.0f\u00B0", prefs.getFloat("prefs_min_water_temp", MIN_WATER_TEMP_CELSIUS)));
					        minWaterTempPref.setMinValue(-20f);
					        minWaterTempPref.setMaxValue(120f);
					        minWaterTempPref.setScale(2.5f);
					        minWaterTempPref.setDefaultValue(prefs.getFloat("prefs_min_water_temp", MIN_WATER_TEMP_CELSIUS));
	
					        maxWaterTempPref.setSummary(String.format("%.0f\u00B0", prefs.getFloat("prefs_max_water_temp", MAX_WATER_TEMP_CELSIUS)));
					        maxWaterTempPref.setMinValue(-20f);
					        maxWaterTempPref.setMaxValue(120f);
					        maxWaterTempPref.setScale(2.5f);
					        maxWaterTempPref.setDefaultValue(prefs.getFloat("prefs_max_water_temp", MAX_WATER_TEMP_CELSIUS));
					        
					        break;
			        		
			        	case 1:  // fahrenheit
					        minWaterTempPref.setSummary(String.format("%.0f\u00B0", prefs.getFloat("prefs_min_water_temp", MIN_WATER_TEMP_FAHRENHEIT)));
					        minWaterTempPref.setMinValue(-30f);
					        minWaterTempPref.setMaxValue(220f);
					        minWaterTempPref.setScale(5f);
					        minWaterTempPref.setDefaultValue(prefs.getFloat("prefs_min_water_temp", MIN_WATER_TEMP_FAHRENHEIT));
	
					        maxWaterTempPref.setSummary(String.format("%.0f\u00B0", prefs.getFloat("prefs_max_water_temp", MAX_WATER_TEMP_FAHRENHEIT)));
					        maxWaterTempPref.setMinValue(-30f);
					        maxWaterTempPref.setMaxValue(220f);
					        maxWaterTempPref.setScale(5f);
					        maxWaterTempPref.setDefaultValue(prefs.getFloat("prefs_max_water_temp", MAX_WATER_TEMP_FAHRENHEIT));
			        }
	
			        tempPrefCat.addPreference(waterTempPref);
			        tempPrefCat.addPreference(minWaterTempPref);
			        tempPrefCat.addPreference(maxWaterTempPref);
		     
            	PreferenceCategory loggingPrefCat = new PreferenceCategory(ctx);
            	loggingPrefCat.setTitle(R.string.prefs_logging_category);
            	alertsPref.addPreference(loggingPrefCat);

			        final CheckBoxPreference loggingPref = new CheckBoxPreference(ctx);
			        
			        loggingPref.setPersistent(true);
			        loggingPref.setKey("prefs_log_all");
			        loggingPref.setDefaultValue(false);
			        loggingPref.setSummaryOn(R.string.enabled);
			        loggingPref.setSummaryOff(R.string.disabled);
			        loggingPref.setTitle(R.string.prefs_logging_enabled);
			        loggingPref.setEnabled(true);
			        
			        loggingPrefCat.addPreference(loggingPref);

	        setPreferenceScreen(alertsPref);
        }
    }
	    
	    @Override
	    public void onBuildHeaders(List<Header> target) {

	    	Header generalHead = new Header();
	    	generalHead.titleRes = R.string.prefs_general_pref;	    	
	    	generalHead.fragment="com.shellware.adaptronic.adaptive.tuner.preferences.AdaptivePreferences$GeneralFragment";
	    	generalHead.iconRes = R.drawable.action_settings;
	    	target.add(generalHead);
	    	
	    	Header alertsHead = new Header();
	    	alertsHead.titleRes = R.string.prefs_alerts_pref;	    	
	    	alertsHead.fragment="com.shellware.adaptronic.adaptive.tuner.preferences.AdaptivePreferences$AlertsFragment";
	    	alertsHead.iconRes = R.drawable.states_warning;
	    	target.add(alertsHead);
	    	
	    }


//    private static SeekBarPreference OpacityPreference(final String title, 
//											    final String key) {
//    
//        final SeekBarPreference newPref = new SeekBarPreference(ctx);
//        
//        newPref.setPersistent(false);
//        newPref.setTitle(title);
//        newPref.setSummary(String.format("%.0f%%", prefs.getFloat(key, 100f)));
//        newPref.setSuffix("%");
//        newPref.setMinValue(0f);
//        newPref.setMaxValue(100f);
//        newPref.setScale(10f);
//        newPref.setDefaultValue(prefs.getFloat(key, 100f));
//        
//        newPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//			public boolean onPreferenceChange(Preference arg0, Object arg1) {
//				newPref.setSummary(String.format("%.0f%%", (Float) arg1));
//				edit.putFloat(key, (Float) arg1);
//				edit.commit();
//				return true;
//			}
//        });
//        
//    	return newPref;
//    }
//    
//    private static Preference simplePreference(final int title, final String summary) {
//    	return simplePreference(res.getString(title), summary);
//    }
//    private static Preference simplePreference(final int title, final int summary) {
//    	return simplePreference(res.getString(title), res.getString(summary));
//    }
//    private static Preference simplePreference(final String title, final String summary) {
//	    Preference simplePref = new Preference(ctx);
//	    simplePref.setTitle(title);
//	    simplePref.setSummary(summary);
//	    return simplePref;
//    }
//    	        
//
//    private static void restartPreferences() {
//		
//    	ctx.finish();
//    }
    
    @Override
    protected void onStop() {
        super.onStop();        
        // commit any sharedpreferences changes
        edit.commit();
    }
    
    @Override
    protected boolean isValidFragment(String fragmentName) {
    	  return GeneralFragment.class.getName().equals(fragmentName) || AlertsFragment.class.getName().equals(fragmentName);
	}
}

