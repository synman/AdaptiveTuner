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
import java.util.Locale;

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
	
	public void addLogItem(LogItem item) {
		LogItem newItem = newLogItem();
		
		newItem.afr = item.afr;
		newItem.auxt = item.auxt;
		newItem.closedLoop = item.closedLoop;
		newItem.learningFLoad = item.learningFLoad;
		newItem.learningFRpm = item.learningFRpm;
		newItem.learningFWait = item.learningFWait;
		newItem.learningILoad = item.learningILoad;
		newItem.learningIRpm = item.learningIRpm;
		newItem.learningIWait = item.learningIWait;
		newItem.map = item.map;
		newItem.mat = item.mat;
		newItem.referenceAfr = item.referenceAfr;
		newItem.rpm = item.rpm;
		newItem.targetAfr = item.targetAfr;
		newItem.tps = item.tps;
		newItem.wat = item.wat;
		newItem.knock = item.knock;
		newItem.volts = item.volts;
		newItem.fuelpres = item.fuelpres;
		newItem.oilpres = item.oilpres;
		newItem.auxpres = item.auxpres;
		
		items.add(newItem);
	}
	
	public class LogItem {

		private final long timestamp = System.currentTimeMillis();
		
		private int rpm = 0;
		private int map = 0;
		private int wat = 0;
		private int mat = 0;
		private int auxt = 0;
		private int tps = 0;		
		private int knock = 0;
		private int oilpres = 0;
		private int fuelpres = 0;
		private int auxpres = 0;

		//TODO: add safety cut functions
		
		private float afr = 0f;
		private float referenceAfr = 0f;
		private float targetAfr = 0f;
		private float volts = 0f;
		private float channelOneVolts = 0f;
				
		private boolean learningIWait = false;
		private boolean learningIRpm = false;
		private boolean learningILoad = false;
		
		private boolean learningFWait = false;
		private boolean learningFRpm = false;
		private boolean learningFLoad = false;
		
		private boolean closedLoop = false;
				
		public int getRpm() {
			return rpm;
		}

		public void setRpm(final int rpm) {
			this.rpm = rpm;
		}

		public int getMap() {
			return map;
		}

		public void setMap(final int map) {
			this.map = map;
		}

		public int getWat() {
			return wat;
		}

		public void setWat(final int wat) {
			this.wat = wat;
		}

		public int getMat() {
			return mat;
		}

		public void setMat(final int mat) {
			this.mat = mat;
		}

		public int getAuxt() {
			return auxt;
		}

		public void setAuxt(final int auxt) {
			this.auxt = auxt;
		}

		public int getTps() {
			return tps;
		}

		public void setTps(final int tps) {
			this.tps = tps;
		}

		public float getAfr() {
			return afr;
		}

		public void setAfr(final float afr) {
			this.afr = afr;
		}

		public float getReferenceAfr() {
			return referenceAfr;
		}

		public void setReferenceAfr(final float referenceAfr) {
			this.referenceAfr = referenceAfr;
		}

		public float getTargetAfr() {
			return targetAfr;
		}

		public void setTargetAfr(final float targetAfr) {
			this.targetAfr = targetAfr;
		}
		
		public float getVolts() {
			return volts;
		}

		public void setVolts(float volts) {
			this.volts = volts;
		}

		public int getKnock() {
			return knock;
		}

		public void setKnock(int knock) {
			this.knock = knock;
		}
		
		public int getOilpres() {
			return oilpres;
		}

		public void setOilpres(int oilpres) {
			this.oilpres = oilpres;
		}

		public int getFuelpres() {
			return fuelpres;
		}

		public void setFuelpres(int fuelpres) {
			this.fuelpres = fuelpres;
		}

		public int getAuxpres() {
			return auxpres;
		}

		public void setAuxpres(int auxpres) {
			this.auxpres = auxpres;
		}

		public boolean isLearningIWait() {
			return learningIWait;
		}

		public void setLearningIWait(final boolean learningIWait) {
			this.learningIWait = learningIWait;
		}

		public boolean isLearningIRpm() {
			return learningIRpm;
		}

		public void setLearningIRpm(final boolean learningIRpm) {
			this.learningIRpm = learningIRpm;
		}

		public boolean isLearningILoad() {
			return learningILoad;
		}

		public void setLearningILoad(final boolean learningILoad) {
			this.learningILoad = learningILoad;
		}

		public boolean isLearningFWait() {
			return learningFWait;
		}

		public void setLearningFWait(final boolean learningFWait) {
			this.learningFWait = learningFWait;
		}

		public boolean isLearningFRpm() {
			return learningFRpm;
		}

		public void setLearningFRpm(final boolean learningFRpm) {
			this.learningFRpm = learningFRpm;
		}

		public boolean isLearningFLoad() {
			return learningFLoad;
		}

		public void setLearningFLoad(final boolean learningFLoad) {
			this.learningFLoad = learningFLoad;
		}

		public boolean isClosedLoop() {
			return closedLoop;
		}

		public void setClosedLoop(final boolean closedLoop) {
			this.closedLoop = closedLoop;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public String getLogString() {
			
			final String str = String.format(Locale.US, "%d, %d, %d, %d, %.1f, %.1f, %.1f, %d, %d, %d, %d, %d, %.1f, %d, %d, %d\n", 
												timestamp,
												rpm,
												map,
												closedLoop ? 1 : 0,
												targetAfr,
												afr,
												referenceAfr,
												tps,
												wat,
												mat,
												auxt,
												knock,
												volts,
												fuelpres,
												oilpres,
												auxpres);
			
			return str;										
		}
		
		public byte[] getLogBytes() {
			return getLogString().getBytes();			
		}
	}
}

