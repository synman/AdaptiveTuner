package com.shellware.adaptronic.adaptive.tuner;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SmallFontArrayAdapter extends ArrayAdapter<String> {

	public SmallFontArrayAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
//		final TextView tv = (TextView) super.getView(position, convertView, parent);
//		tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tv.getTextSize() * .75f);
		
		return super.getView(position, convertView, parent);
	}
}
