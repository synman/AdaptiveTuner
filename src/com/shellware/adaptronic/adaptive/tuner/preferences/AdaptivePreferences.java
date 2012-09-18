package com.shellware.adaptronic.adaptive.tuner.preferences;

import android.bluetooth.BluetoothAdapter;
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
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;

import com.shellware.adaptronic.adaptive.tuner.MainActivity;
import com.shellware.adaptronic.adaptive.tuner.R;

public class AdaptivePreferences extends PreferenceActivity {

	private static SharedPreferences prefs ;
	private static Editor edit;
		
	private static AdaptivePreferences ctx;
	private static Resources res;
	
    // Local Bluetooth adapter
    private static BluetoothAdapter bta = null;
    
    public static final float MIN_WATER_TEMP_CELCIUS = 72f;
    public static final float MAX_WATER_TEMP_CELCIUS = 98f;
    
    public static final float MIN_WATER_TEMP_FAHRENHEIT = 160f;
    public static final float MAX_WATER_TEMP_FAHRENHEIT = 210f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    	super.onCreate(savedInstanceState);
        
        if (MainActivity.DEBUG_MODE) Log.d(MainActivity.TAG, "Preferences onCreate");

        // Get local Bluetooth adapter
        bta = BluetoothAdapter.getDefaultAdapter();
	    
        ctx = this;
        res = getResources();

    	prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	edit = prefs.edit();

        setTitle(R.string.prefs_title);
    }
    
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		
        // Handle item selection
        switch (item.getItemId()) {

        }
        return true;
	}
	
    /* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();

	    Log.d(MainActivity.TAG, "Preferences onResume");

    	setPreferenceScreen(createPreferenceHierarchy());   
	}


	  private PreferenceScreen createPreferenceHierarchy() {  	
  	
	      // Root
	      PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
	      root.setTitle(R.string.prefs_title);
	
	      PreferenceCategory generalPrefCat = new PreferenceCategory(this);
	      generalPrefCat.setTitle(R.string.prefs_general_pref);
	      root.addPreference(generalPrefCat);
	
//	            PreferenceScreen generalPref = getPreferenceManager().createPreferenceScreen(ctx);
//	            generalPref.setKey("prefs_general");
//	            generalPref.setTitle(R.string.prefs_general_pref);
//	            generalPref.setSummary(R.string.prefs_general_summary);
//	            generalPref.setTitle(R.string.prefs_general_pref);
//	            generalPrefCat.addPreference(generalPref);
		        
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
							        edit.putFloat("prefs_min_water_temp", MIN_WATER_TEMP_CELCIUS);
							        edit.putFloat("prefs_max_water_temp", MAX_WATER_TEMP_CELCIUS);
							        break;
						        case 1:
							        edit.putFloat("prefs_min_water_temp", MIN_WATER_TEMP_FAHRENHEIT);
							        edit.putFloat("prefs_max_water_temp", MAX_WATER_TEMP_FAHRENHEIT);
					        }
					        
					        edit.commit();
							return true;
						}
			        });
			        generalPrefCat.addPreference(uomTempPref);

//		            PreferenceScreen alertsPref = getPreferenceManager().createPreferenceScreen(ctx);
//		            alertsPref.setKey("prefs_alerts");
//		            alertsPref.setTitle(R.string.prefs_alerts_pref);

				      PreferenceCategory alertsPrefCat = new PreferenceCategory(this);
				      alertsPrefCat.setTitle(R.string.prefs_alerts_pref);
				      root.addPreference(alertsPrefCat);

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
				        afrLoggingPref.setTitle(R.string.prefs_afr_alarm_loging);
				        afrLoggingPref.setEnabled(prefs.getBoolean("prefs_afrnottarget_pref", false));
				        
				        afrNotTargetPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
							public boolean onPreferenceChange(Preference arg0, Object arg1) {
								afrNotTargetTolPref.setEnabled((Boolean) arg1);
								afrLoggingPref.setEnabled((Boolean) arg1);
								return true;
							}
				        });
			         
			        alertsPrefCat.addPreference(afrNotTargetPref);
			        alertsPrefCat.addPreference(afrNotTargetTolPref);
			        alertsPrefCat.addPreference(afrLoggingPref);
				        
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
				        	case 0:  // celcius
						        minWaterTempPref.setSummary(String.format("%.0f\u00B0", prefs.getFloat("prefs_min_water_temp", MIN_WATER_TEMP_CELCIUS)));
						        minWaterTempPref.setMinValue(-20f);
						        minWaterTempPref.setMaxValue(120f);
						        minWaterTempPref.setScale(2.5f);
						        minWaterTempPref.setDefaultValue(prefs.getFloat("prefs_min_water_temp", MIN_WATER_TEMP_CELCIUS));

						        maxWaterTempPref.setSummary(String.format("%.0f\u00B0", prefs.getFloat("prefs_max_water_temp", MAX_WATER_TEMP_CELCIUS)));
						        maxWaterTempPref.setMinValue(-20f);
						        maxWaterTempPref.setMaxValue(120f);
						        maxWaterTempPref.setScale(2.5f);
						        maxWaterTempPref.setDefaultValue(prefs.getFloat("prefs_max_water_temp", MAX_WATER_TEMP_CELCIUS));

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

			        alertsPrefCat.addPreference(waterTempPref);
			        alertsPrefCat.addPreference(minWaterTempPref);
			        alertsPrefCat.addPreference(maxWaterTempPref);
			        
	        return root;
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
}

