/*
 *   Copyright 2013 Shell M. Shrader
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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.ImageView;

import com.shellware.adaptronic.adaptive.tuner.R;


public class CellValueWidget extends ImageView {
	
	private enum CellValueWidgetChangeType {
		INCREMENT_SMALL,
		DECREMENT_SMALL,
		INCREMENT_BIG,
		DECREMENT_BIG 
	}
	
	private EditText editText;
	private CellValueWidgetChangeType pendingChangeType; 
	
	public CellValueWidget(Context context) {
		super(context);
	}

	public CellValueWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public CellValueWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setEditText(final EditText editText) {
		this.editText = editText;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				DetermineChangeType(event.getX(), event.getY());
				break;
			case MotionEvent.ACTION_UP:
				ProcessChange();
				break;
		}
		
		return true;
	}

	private void DetermineChangeType(final float x, final float y) {
		
		final float smallWidth = this.getWidth() / 2 - this.getWidth() / 4;
		final float smallHeight = this.getHeight() / 2 - this.getHeight() / 4;
		
		// determine which zone was pressed
		if (x > smallWidth && x < this.getWidth() - smallWidth && y > smallHeight && y < this.getHeight() - smallHeight) {
			if (y < this.getHeight() / 2) {
				this.setImageResource(R.drawable.cell_value_widget_small_up);
				pendingChangeType = CellValueWidgetChangeType.INCREMENT_SMALL;
			} else {
				this.setImageResource(R.drawable.cell_value_widget_small_down);
				pendingChangeType = CellValueWidgetChangeType.DECREMENT_SMALL;
			}
		} else {
			if (y < this.getHeight() / 2) {
				this.setImageResource(R.drawable.cell_value_widget_big_up);
				pendingChangeType = CellValueWidgetChangeType.INCREMENT_BIG;
			} else {
				this.setImageResource(R.drawable.cell_value_widget_big_down);
				pendingChangeType = CellValueWidgetChangeType.DECREMENT_BIG;
			}
		}	
	}
	
	
	private void ProcessChange() {
		this.setImageResource(R.drawable.cell_value_widget);

		final float value;
		try {
			value = Float.parseFloat(editText.getText().toString());
		} catch (Exception ex) {
			return;
		}

		switch (pendingChangeType) {
			case INCREMENT_SMALL:
				editText.setText(String.format("%.2f", value + .01f));
				break;
			case DECREMENT_SMALL:
				editText.setText(String.format("%.2f", value - .01f));
				break;
			case INCREMENT_BIG:
				editText.setText(String.format("%.2f", value + 1f));
				break;
			case DECREMENT_BIG:
				editText.setText(String.format("%.2f", value - 1f));
				break;
		}
	}
		
}
