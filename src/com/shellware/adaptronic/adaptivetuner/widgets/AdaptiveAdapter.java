package com.shellware.adaptronic.adaptivetuner.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class AdaptiveAdapter extends ArrayAdapter<String> {

	private final static int ORIENTATION_PHONE = 0;
	private final static int ORIENTATION_PHONE_PORTRAIT = 1;
//	private final static int ORIENTATION_PHABLET = 2;
//	private final static int ORIENTATION_PHABLET_PORTRAIT = 3;
//	private final static int ORIENTATION_TABLET = 4;
//	private final static int ORIENTATION_TABLET_PORTRAIT = 5;
	
	private Context ctx;
	private LayoutInflater inflater;
	private int textViewResourceId;
	
	private Typeface typeface;
	
	private int orientation;

	public AdaptiveAdapter(Context ctx, int textViewResourceId) {
		super(ctx, textViewResourceId);
		
		this.ctx = ctx;
		inflater = (LayoutInflater)this.ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.textViewResourceId = textViewResourceId;
	}

	public AdaptiveAdapter(Context ctx, int textViewResourceId, Typeface typeface) {
		super(ctx, textViewResourceId);
		
		this.ctx = ctx;
		inflater = (LayoutInflater)this.ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.textViewResourceId = textViewResourceId;
		this.typeface = typeface;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		if (convertView == null) {
			convertView = (TextView) inflater.inflate(textViewResourceId, null);
			orientation = Integer.parseInt((String) ((TextView) convertView).getText());

			((TextView) convertView).setTypeface(typeface);
			
			if (orientation == ORIENTATION_PHONE || orientation == ORIENTATION_PHONE_PORTRAIT) {
				((TextView) convertView).setTextSize(16);
				((TextView) convertView).setLineSpacing(2, 1);			
			} else {
				((TextView) convertView).setTextSize(28);
				((TextView) convertView).setLineSpacing(4, 1);			
			}
		}
		
		return super.getView(position, convertView, parent);
	}
}
