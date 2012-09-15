/* The following code was originally written by Matthew Wiggins 
 * and later refactored by Shell M. Shrader
 * It is released under the APACHE 2.0 license 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.shellware.adaptronic.adaptive.tuner.preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.shellware.adaptronic.adaptive.tuner.R;

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {

	private float startingValue = 0;
	private float barValue = 0;
	private float displayValue = 0;
	private String suffix = "";
	private float minValue = 0;
	private float maxValue = 0;
	private float scale = 1;
	
	@SuppressWarnings("unused")
	private Context ctx;
	
	private TextView splashTextView;
	private TextView valueTextView;
	private SeekBar seekBarView;
	
	public SeekBarPreference(Context context) {
		super(context, null);
		this.ctx = context;
	}

	@Override
	protected View onCreateDialogView() {		
		setDialogLayoutResource(R.layout.seekbar);
		return super.onCreateDialogView();
	}

	@Override
	protected void onBindDialogView(View view) {
		
		seekBarView = (SeekBar) view.findViewById(R.id.seek);
		seekBarView.setMax( (int) ((maxValue - minValue) / scale));
		seekBarView.setProgress((int) barValue);
		seekBarView.setOnSeekBarChangeListener(this);
		
		valueTextView = (TextView) view.findViewById(R.id.currentValue);
		valueTextView.setText(String.format("%.2f%s", displayValue, suffix));
		
		splashTextView = (TextView) view.findViewById(R.id.splashText);
		splashTextView.setText(getTitle());
		
		//Log.d(MainActivity.TAG, "bound");
		super.onBindDialogView(view);
	}

	public void onProgressChanged(SeekBar arg0, int progress, boolean fromUser) {
		barValue = progress;
		displayValue = barValue * scale + minValue;
		valueTextView.setText(String.format("%.2f%s", displayValue, suffix));
		//Log.d(MainActivity.TAG, String.format("changed value %.2f/%.2f", displayValue, barValue));
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			callChangeListener(displayValue);
			startingValue = displayValue;
			//Log.d(MainActivity.TAG, String.format("close positive value %.2f/%.2f", displayValue, barValue));
		} else {

			displayValue = startingValue;
			barValue = (displayValue - minValue) / scale;
			seekBarView.setProgress((int) barValue);

			//Log.d(MainActivity.TAG, String.format("close negative value %.2f/%.2f", displayValue, barValue));
		}
		super.onDialogClosed(positiveResult);
	}

	public void onStartTrackingTouch(SeekBar arg0) {}
	public void onStopTrackingTouch(SeekBar arg0) {}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		super.onSetInitialValue(restorePersistedValue, defaultValue);
		displayValue = (Float) defaultValue;
		barValue = (displayValue - minValue) / scale;
		startingValue = displayValue;

		//Log.d(MainActivity.TAG, String.format("initial value %.2f/%.2f", displayValue, barValue));
	}

	public void setMinValue(float minValue) {
		this.minValue = minValue;
	}

	public void setMaxValue(float maxValue) {
		this.maxValue = maxValue;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}
}