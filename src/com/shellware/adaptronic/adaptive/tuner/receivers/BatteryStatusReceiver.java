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
package com.shellware.adaptronic.adaptive.tuner.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.BatteryManager;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.shellware.adaptronic.adaptive.tuner.logging.AdaptiveLogger;
import com.shellware.adaptronic.adaptive.tuner.services.ConnectionService;

public class BatteryStatusReceiver extends BroadcastReceiver {
	
	private static AdaptiveLogger logger = new AdaptiveLogger(AdaptiveLogger.DEFAULT_LEVEL, AdaptiveLogger.DEFAULT_TAG);

	private final static int BATTERY_PLUGGED_UNPLUGGED = 0;
	
	@Override
	public void onReceive(final Context ctx, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)){
			
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);	
			
			final int status = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, BATTERY_PLUGGED_UNPLUGGED);
			final int lastStatus = prefs.getInt("last_battery_status", BATTERY_PLUGGED_UNPLUGGED);
			
			if (status != BATTERY_PLUGGED_UNPLUGGED && lastStatus == BATTERY_PLUGGED_UNPLUGGED) {
				
				final boolean enabled = prefs.getBoolean("prefs_connect_on_charge", false);
		    	final boolean waiting = prefs.getBoolean("prefs_connect_on_charge_waiting", false);
				
				if (enabled && !waiting && ConnectionService.state == ConnectionService.State.DISCONNECTED) {
			    	
			    	final int waitTime = (int) prefs.getFloat("prefs_connect_on_charge_wait_time", 30);

			    	final Editor edit = prefs.edit();
		    		edit.putBoolean("prefs_connect_on_charge_waiting", true);
		    		edit.commit();
		    		
		    		logger.log("BatteryStatusReceiver submitting auto connect intent");				    		

			    	new Handler().postDelayed(new Runnable() {

						public void run() {

					    	final String name = prefs.getString("prefs_remote_name", "");
					    	final String addr = prefs.getString("prefs_remote_mac", "");						    		
				        	final boolean waiting = prefs.getBoolean("prefs_connect_on_charge_waiting", false);

				        	if (waiting && addr.length() > 0) {
					        	Intent service = new Intent(ConnectionService.ACTION_CONNECT_BT);
					        	service.putExtra("name", name);
					        	service.putExtra("addr", addr);
					    		
				        		ctx.startService(service);
				        		
					    		logger.log("BatteryStatusReceiver auto connect intent sent");
				        	}						        	

				        	edit.putBoolean("prefs_connect_on_charge_waiting", false);
				    		edit.commit();
					    	
						}
						
					} , waitTime * 1000);
				}
			}
			
			final Editor edit = prefs.edit();
			edit.putInt("last_battery_status", status);
			edit.commit();

    		logger.log("BatteryStatusReceiver charge status: " + status);
		}
	}

}

