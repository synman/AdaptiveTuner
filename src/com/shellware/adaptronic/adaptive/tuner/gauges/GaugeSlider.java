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
package com.shellware.adaptronic.adaptive.tuner.gauges;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.shellware.adaptronic.adaptive.tuner.MainActivity;

public class GaugeSlider extends ImageView {

	private static final String TAG = MainActivity.TAG;
	private static final boolean DEBUG = MainActivity.DEBUG;

	private int minValue = 0;
	private int maxValue = 360;
	
	private int usableUnits = 0;
	
	private String suffix = "";

	public GaugeSlider(Context context) {
		super(context);
	}
	public GaugeSlider(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public GaugeSlider(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setValue(final float value) {

		if (value < minValue || value > maxValue) return;
		
		if (usableUnits == 0) {
			final RelativeLayout parent = (RelativeLayout) getParent();
			usableUnits = parent.getWidth();
			if (DEBUG) Log.d(TAG, "slider usable: " + usableUnits);
		}
				
		final float multiplier = usableUnits / (float) (maxValue - minValue);
		
		final LayoutParams params = (LayoutParams) getLayoutParams();
		params.leftMargin = (int) ((value - minValue) * multiplier) - (getWidth() / 2); 
		
		setLayoutParams(params);
		
		if (DEBUG) Log.d(TAG, "slider : " + value + " - " + params.leftMargin);
		this.setLayoutParams(params);
		
		final RelativeLayout frame = (RelativeLayout) getParent().getParent();
		final TextView text = (TextView) frame.getChildAt(frame.getChildCount() - 1);
		text.setText(String.format("%.0f%s", value, suffix));
	}
	
	public int getMinValue() {
		return minValue;
	}
	public void setMinValue(int minValue) {
		RelativeLayout frame = (RelativeLayout) getParent().getParent();
		TextView text = (TextView) frame.getChildAt(frame.getChildCount() - 3);
		text.setText(String.format("%d", minValue));
		
		this.minValue = minValue;
	}
	public int getMaxValue() {
		return maxValue;
	}
	public void setMaxValue(int maxValue) {
		RelativeLayout frame = (RelativeLayout) getParent().getParent();
		TextView text = (TextView) frame.getChildAt(frame.getChildCount() - 2);
		text.setText(String.format("%d", maxValue));
		
		this.maxValue = maxValue;
	}
	public String getSuffix() {
		return suffix;
	}
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}
}
