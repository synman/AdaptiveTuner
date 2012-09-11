package com.shellware.adaptronic.adaptive.tuner.preferences;

import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
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
	      root.setTitle(R.string.menu_prefs);
	
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
							return true;
						}
			        });
			        generalPrefCat.addPreference(uomTempPref);
			        
	        return root;
    }
	

    private static SeekBarPreference OpacityPreference(final String title, 
											    final String key) {
    
        final SeekBarPreference newPref = new SeekBarPreference(ctx);
        
        newPref.setPersistent(false);
        newPref.setTitle(title);
        newPref.setSummary(String.format("%.0f%%", prefs.getFloat(key, 100f)));
        newPref.setSuffix("%");
        newPref.setMinValue(0f);
        newPref.setMaxValue(100f);
        newPref.setScale(10f);
        newPref.setDefaultValue(prefs.getFloat(key, 100f));
        
        newPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				newPref.setSummary(String.format("%.0f%%", (Float) arg1));
				edit.putFloat(key, (Float) arg1);
				edit.commit();
				return true;
			}
        });
        
    	return newPref;
    }
    
    private static Preference simplePreference(final int title, final String summary) {
    	return simplePreference(res.getString(title), summary);
    }
    private static Preference simplePreference(final int title, final int summary) {
    	return simplePreference(res.getString(title), res.getString(summary));
    }
    private static Preference simplePreference(final String title, final String summary) {
	    Preference simplePref = new Preference(ctx);
	    simplePref.setTitle(title);
	    simplePref.setSummary(summary);
	    return simplePref;
    }
    	        

    private static void restartPreferences() {
		
    	ctx.finish();
    }
    
    @Override
    protected void onStop() {
        super.onStop();        
        // commit any sharedpreferences changes
        edit.commit();
    }
}

