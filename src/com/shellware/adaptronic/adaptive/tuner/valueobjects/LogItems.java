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
package com.shellware.adaptronic.adaptive.tuner.valueobjects;

import java.util.ArrayList;

public class LogItems {

	private ArrayList<LogItem> items = new ArrayList<LogItem>(65535);
	
	public ArrayList<LogItem> getItems() {
		return items;
	}

	public void setItems(ArrayList<LogItem> items) {
		this.items = items;
	}

	public LogItem newLogItem() {
		return new LogItem();
	}
	
	public class LogItem {

		private final long timestamp = System.currentTimeMillis();
		
		private int rpm = 0;
		private int map = 0;
		private int wat = 0;
		private int mat = 0;
		private float afr = 0f;
		private float referenceAfr = 0f;
		private float targetAfr = 0f;
		
		private boolean learningIWait = false;
		private boolean learningIRpm = false;
		private boolean learningILoad = false;
		
		private boolean learningFWait = false;
		private boolean learningFRpm = false;
		private boolean learningFLoad = false;
		
		
		public long getTimestamp() {
			return timestamp;
		}
		public int getRpm() {
			return rpm;
		}
		public void setRpm(int rpm) {
			this.rpm = rpm;
		}
		public int getMap() {
			return map;
		}
		public void setMap(int map) {
			this.map = map;
		}
		public int getWat() {
			return wat;
		}
		public void setWat(int wat) {
			this.wat = wat;
		}
		public int getMat() {
			return mat;
		}
		public void setMat(int mat) {
			this.mat = mat;
		}
		public float getAfr() {
			return afr;
		}
		public void setAfr(float afr) {
			this.afr = afr;
		}
		public float getTargetAfr() {
			return targetAfr;
		}
		public void setTargetAfr(float targetAfr) {
			this.targetAfr = targetAfr;
		}
		public boolean isLearningIWait() {
			return learningIWait;
		}
		public void setLearningIWait(boolean learningIWait) {
			this.learningIWait = learningIWait;
		}
		public boolean isLearningIRpm() {
			return learningIRpm;
		}
		public void setLearningIRpm(boolean learningIRpm) {
			this.learningIRpm = learningIRpm;
		}
		public boolean isLearningILoad() {
			return learningILoad;
		}
		public void setLearningILoad(boolean learningILoad) {
			this.learningILoad = learningILoad;
		}
		public boolean isLearningFWait() {
			return learningFWait;
		}
		public void setLearningFWait(boolean learningFWait) {
			this.learningFWait = learningFWait;
		}
		public boolean isLearningFRpm() {
			return learningFRpm;
		}
		public void setLearningFRpm(boolean learningFRpm) {
			this.learningFRpm = learningFRpm;
		}
		public boolean isLearningFLoad() {
			return learningFLoad;
		}
		public void setLearningFLoad(boolean learningFLoad) {
			this.learningFLoad = learningFLoad;
		}
		public float getReferenceAfr() {
			return referenceAfr;
		}
		public void setReferenceAfr(float referenceAfr) {
			this.referenceAfr = referenceAfr;
		}
		
		public String getLogString() {
			
			final String str = String.format("%d, %d, %d, %.1f, %.1f, %.1f, %d, %d\n", 
												timestamp,
												rpm,
												map,
												targetAfr,
												afr,
												referenceAfr,
												wat,
												mat);
			
			return str;
												
		}
		
		public byte[] getLogBytes() {
			
			final String str = String.format("%d, %d, %d, %.1f, %.1f, %.1f, %d, %d\n", 
												timestamp,
												rpm,
												map,
												targetAfr,
												afr,
												referenceAfr,
												wat,
												mat);
			
			return str.getBytes();
												
		}
	}
}

