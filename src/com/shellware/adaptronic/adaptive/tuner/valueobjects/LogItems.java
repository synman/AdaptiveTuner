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

	public static LogItem newLogItem() {
		return new LogItem();
	}
	
	public static class LogItem {

		private final long timestamp = System.currentTimeMillis();
		
		private int rpm = 0;
		private int map = 0;
		private int wat = 0;
		private int mat = 0;
		private int tps = 0;
		
		private float afr = 0f;
		private float referenceAfr = 0f;
		private float targetAfr = 0f;
		
		private boolean learningIWait = false;
		private boolean learningIRpm = false;
		private boolean learningILoad = false;
		
		private boolean learningFWait = false;
		private boolean learningFRpm = false;
		private boolean learningFLoad = false;
		
		private boolean closedLoop = false;
				
		public synchronized int getRpm() {
			return rpm;
		}

		public synchronized void setRpm(final int rpm) {
			this.rpm = rpm;
		}

		public synchronized int getMap() {
			return map;
		}

		public synchronized void setMap(final int map) {
			this.map = map;
		}

		public synchronized int getWat() {
			return wat;
		}

		public synchronized void setWat(final int wat) {
			this.wat = wat;
		}

		public synchronized int getMat() {
			return mat;
		}

		public synchronized void setMat(final int mat) {
			this.mat = mat;
		}

		public synchronized int getTps() {
			return tps;
		}

		public synchronized void setTps(final int tps) {
			this.tps = tps;
		}

		public synchronized float getAfr() {
			return afr;
		}

		public synchronized void setAfr(final float afr) {
			this.afr = afr;
		}

		public synchronized float getReferenceAfr() {
			return referenceAfr;
		}

		public synchronized void setReferenceAfr(final float referenceAfr) {
			this.referenceAfr = referenceAfr;
		}

		public synchronized float getTargetAfr() {
			return targetAfr;
		}

		public synchronized void setTargetAfr(final float targetAfr) {
			this.targetAfr = targetAfr;
		}

		public synchronized boolean isLearningIWait() {
			return learningIWait;
		}

		public synchronized void setLearningIWait(final boolean learningIWait) {
			this.learningIWait = learningIWait;
		}

		public synchronized boolean isLearningIRpm() {
			return learningIRpm;
		}

		public synchronized void setLearningIRpm(final boolean learningIRpm) {
			this.learningIRpm = learningIRpm;
		}

		public synchronized boolean isLearningILoad() {
			return learningILoad;
		}

		public synchronized void setLearningILoad(final boolean learningILoad) {
			this.learningILoad = learningILoad;
		}

		public synchronized boolean isLearningFWait() {
			return learningFWait;
		}

		public synchronized void setLearningFWait(final boolean learningFWait) {
			this.learningFWait = learningFWait;
		}

		public synchronized boolean isLearningFRpm() {
			return learningFRpm;
		}

		public synchronized void setLearningFRpm(final boolean learningFRpm) {
			this.learningFRpm = learningFRpm;
		}

		public synchronized boolean isLearningFLoad() {
			return learningFLoad;
		}

		public synchronized void setLearningFLoad(final boolean learningFLoad) {
			this.learningFLoad = learningFLoad;
		}

		public synchronized boolean isClosedLoop() {
			return closedLoop;
		}

		public synchronized void setClosedLoop(final boolean closedLoop) {
			this.closedLoop = closedLoop;
		}

		public synchronized long getTimestamp() {
			return timestamp;
		}

		public String getLogString() {
			
			final String str = String.format("%d, %d, %d, %d, %.1f, %.1f, %.1f, %d, %d, %d\n", 
												timestamp,
												rpm,
												map,
												closedLoop ? 1 : 0,
												targetAfr,
												afr,
												referenceAfr,
												tps,
												wat,
												mat);
			
			return str;										
		}
		
		public byte[] getLogBytes() {
			return getLogString().getBytes();			
		}
	}
}

