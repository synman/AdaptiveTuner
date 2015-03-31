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
package com.shellware.adaptronic.adaptive.tuner.logging;

import android.util.Log;

public final class AdaptiveLogger {
	
	private AdaptiveLogger() {
		super();
	}

	public static enum Level {
		DEBUG,
		INFO,
		ERROR,
		NONE
	}
	
	public static final Level DEFAULT_LEVEL = Level.DEBUG;
	public static final String DEFAULT_TAG = "Adaptive";
	
	// set releaseVersion to true when releasing Adaptive Tuner
	// to Google Play
	private static boolean releaseVersion = true;
	
	private static Level level = DEFAULT_LEVEL;
	private static String tag = DEFAULT_TAG;
//	
//	public AdaptiveLogger() {	
//	}

	public static void log(String msg) {
		log(level, tag, msg);
	}
	
	public static void log(String tag, String msg) {
		log(level, tag, msg);
	}

	public static void log(Level level, String msg) {
		log(level, tag, msg);
	}
	
	public static void log(Level level, String tag, String msg) {
		
		if (releaseVersion) return;
		
		switch (level) {
			case DEBUG:
				Log.d(tag, msg);
				break;
				
			case INFO:
				Log.i(tag, msg);
				break;
				
			case ERROR:
				Log.e(tag, msg);
				break;
				
			default:
		}
	}

	/**
	 * @return the level
	 */
	public Level getLevel() {
		return level;
	}

	/**
	 * @param level the level to set
	 */
	public void setLevel(Level level) {
		AdaptiveLogger.level = level;
	}

	/**
	 * @return the tag
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * @param tag the tag to set
	 */
	public void setTag(String tag) {
		AdaptiveLogger.tag = tag;
	}
}
