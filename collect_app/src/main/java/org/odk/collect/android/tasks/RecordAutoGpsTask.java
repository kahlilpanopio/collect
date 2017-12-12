/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.tasks;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import org.odk.collect.android.listeners.AutoGpsRecordingListener;

/**
 * The background task to automatically collect gps
 * This is done when there is a gps question with binding "auto_gps"
 *
 * @author Raghu Mittal (raghu.mittal@gmail.com)
 */
public class RecordAutoGpsTask extends AsyncTask<Object, Void, String> implements LocationListener {
	private static final String t = "RecordAutoGpsTask";
	private Location mLocation;
	private LocationManager mLocationManager;

	private AutoGpsRecordingListener mGpsListener;
	//private String gps;

	@Override
	protected String doInBackground(Object... args) {
		//android.os.Debug.waitForDebugger();
		Looper.prepare();

		mLocationManager = (LocationManager) args[0];
		mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
		Looper.loop(); // start waiting...when this is done, we'll have the location in this.mLocation

		String gpsResult = mLocation.getLatitude() + " " + mLocation.getLongitude() + " "
				+ mLocation.getAltitude() + " " + mLocation.getAccuracy();

		return gpsResult;
	}

	@Override
	protected void onPostExecute(String gpsResult) {
		if (mGpsListener != null) {
			mGpsListener.autoGpsRecordingComplete(gpsResult);
		}
	}

	public void setAutoGpsRecordingListener(AutoGpsRecordingListener agrl) {
		mGpsListener = agrl;
	}

	@Override
	public void onLocationChanged(Location location) {
		// Store the location, then get the current thread's looper and tell it to
		// quit looping so it can continue on doing work with the new location.
		this.mLocation = location;
		Log.i(t, "Recording auto gps: " + System.currentTimeMillis() +
				" onStatusChanged() lat: " + mLocation.getLatitude() + " long: " +
				mLocation.getLongitude() + " alt: " + mLocation.getAltitude() +
				" acc: " + mLocation.getAccuracy());
		Looper.myLooper().quit();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		switch (status) {
			case LocationProvider.AVAILABLE: // When available, gps should be recorded
				Log.i(t, "Recording auto gps: " + System.currentTimeMillis() +
						" onStatusChanged() lat: " + mLocation.getLatitude() + " long: " +
						mLocation.getLongitude() + " alt: " + mLocation.getAltitude() +
						" acc: " + mLocation.getAccuracy());
				Looper.myLooper().quit();
				break;
			case LocationProvider.OUT_OF_SERVICE:
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				break;
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

}