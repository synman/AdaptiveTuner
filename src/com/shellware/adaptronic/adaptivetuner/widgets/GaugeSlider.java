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
package com.shellware.adaptronic.adaptivetuner.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.shellware.adaptronic.adaptivetuner.logging.AdaptiveLogger;

public class GaugeSlider extends ImageView {

	private Context context;
	private int minValue = 0;
	private int maxValue = 360;
	
	private int usableUnits = 0;
	
	private String suffix = "";

	public GaugeSlider(Context context) {
		super(context);
		this.context = context;
	}
	public GaugeSlider(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);	
		this.context = context;
	}
	public GaugeSlider(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	public void setValue(final float value) {

		if (value < minValue || value > maxValue) return;
		
		if (usableUnits == 0) {
			final RelativeLayout parent = (RelativeLayout) getParent();
			usableUnits = parent.getWidth();
			AdaptiveLogger.log("GaugeSlider usable units: " + usableUnits);
		}
				
		final float multiplier = usableUnits / (float) (maxValue - minValue);
		
		final LayoutParams params = (LayoutParams) getLayoutParams();
		params.leftMargin = (int) ((value - minValue) * multiplier) - (getWidth() / 2); 
		
		setLayoutParams(params);
		
		AdaptiveLogger.log("GaugeSlider value and left margin: " + value + " - " + params.leftMargin);
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
		
		final RelativeLayout frame = (RelativeLayout) getParent().getParent();
		
		for (int i = 3;i > 0; i--) {
			final TextView text = (TextView) frame.getChildAt(frame.getChildCount() - i);
			text.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/digital_7.ttf"), Typeface.BOLD);			
		}
	}
}
