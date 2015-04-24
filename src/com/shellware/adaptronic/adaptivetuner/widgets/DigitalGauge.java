package com.shellware.adaptronic.adaptivetuner.widgets;

import com.shellware.adaptronic.adaptive.tuner.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class DigitalGauge extends RelativeLayout {
	
	private RelativeLayout digitalBorder;
	
	private TextView gaugeTitleView;
	private TextView gaugeValueView;
	private TextView gaugeUnitsView;
	
	private String title = "title";
	private String value = "000";
	private String units = "\u00B0 F";
	private int width = 140;

	public DigitalGauge(Context context) {
		super(context);
		init();
	}
	public DigitalGauge(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		getAttributes(attrs);
		init();
	}
	public DigitalGauge(Context context, AttributeSet attrs) {
		super(context, attrs);
		getAttributes(attrs);
		init();
	}
	
	private void getAttributes(AttributeSet attrs) { 
		
		final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DigitalGauge);	 
		final int N = a.getIndexCount();
			
			for (int i = 0; i < N; ++i) {	
			    final int attr = a.getIndex(i);
			    
			    switch (attr) {
			        case R.styleable.DigitalGauge_title:
			            title = a.getString(attr);
			            break;
			        case R.styleable.DigitalGauge_value:
			            value = a.getString(attr);
			            break;
			        case R.styleable.DigitalGauge_units:
			            units = a.getString(attr);
			            break;
			        case R.styleable.DigitalGauge_width:
			        	width = a.getInt(attr, 140);
			        	break;
			    }
			}
			
			a.recycle();
	};
	
	private void init() {
		inflate(getContext(), R.layout.digital_gauge, this);
		
		digitalBorder = (RelativeLayout) findViewById(R.id.digitalborder);
		
		gaugeTitleView = (TextView) findViewById(R.id.digitaltitle);
		gaugeValueView = (TextView) findViewById(R.id.digitalvalue);
		gaugeUnitsView = (TextView) findViewById(R.id.digitalunits);
        
		if (!isInEditMode()) gaugeTitleView.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/digital_7.ttf"), Typeface.NORMAL);
		gaugeTitleView.setTextColor(Color.parseColor("#ffa500"));;
		gaugeTitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
		
		if (!isInEditMode()) gaugeValueView.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/digital_7.ttf"), Typeface.BOLD);
		gaugeValueView.setTextColor(Color.GREEN);
		gaugeValueView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 60);
        
		if (!isInEditMode()) gaugeUnitsView.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/digital_7.ttf"), Typeface.BOLD);
		gaugeUnitsView.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
		gaugeUnitsView.setTextColor(Color.parseColor("#eee9e9"));;
		gaugeUnitsView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		gaugeTitleView.setText(title);
		gaugeValueView.setText(value);
		gaugeUnitsView.setText(units);
		
		// set our width -- 115dp == 2 digits 140dp == 3 digits 170dp == 4 digits
		RelativeLayout.LayoutParams parms = (RelativeLayout.LayoutParams) digitalBorder.getLayoutParams();
		parms.width = convertDpToPx(width);
		digitalBorder.setLayoutParams(parms);
	}
	
	private int convertDpToPx(final int dp) {
		DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
		return  (int) (metrics.density * dp + 0.5f);
	}
	
	public void setValue(final int value) {
		this.value = String.valueOf(value);
		gaugeValueView.setText(this.value);
	}
	
	public void setTitle(final String title) {
		this.title = title;
		gaugeTitleView.setText(this.title);
	}
	
	public void setUnits(final String units) {
		this.units = units;
		gaugeUnitsView.setText(this.units);
	}
}
