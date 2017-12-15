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
import android.util.Log;

import org.odk.collect.android.listeners.AutoGpsRecordingListener;

import java.util.List;

/**
 * The background task to automatically collect gps
 * This is done when there is a gps question with binding "auto_gps"
 *
 * @author Raghu Mittal (raghu.mittal@gmail.com)
 */
public class RecordAutoGpsTask extends AsyncTask<Object, Void, String> {
	private static final String t = "RecordAutoGpsTask";
	private Location mLocation;
	private LocationManager mLocationManager;
	private boolean mGPSOn = false;
	private boolean mNetworkOn = false;
	private boolean isNetworkOnly = false;
	private boolean foundReadingFlag = false;
	private boolean forceCancelTask = false;

	private AutoGpsRecordingListener mGpsListener;
	private AutoGpsLocationListener mAutoGpsLocationListener;
	private String gpsResult;

	public RecordAutoGpsTask(LocationManager locationManager) {
		mLocationManager = locationManager;
	}

	@Override
	protected void onPreExecute() {
		mAutoGpsLocationListener = new AutoGpsLocationListener();

		List<String> providers = mLocationManager.getProviders(true);
		for (String provider : providers) {
			if (provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
				mGPSOn = true;
			}
			if (provider.equalsIgnoreCase(LocationManager.NETWORK_PROVIDER)) {
				mNetworkOn = true;
			}
		}

		if (!mGPSOn && !mNetworkOn) {
			// Location services off right now
			// Probably, the user shut down location services on opening the form
			//mGpsListener.promptUserToTurnOnLocationServices();
			gpsResult = null;
		} else if (!isNetworkOnly && mGPSOn) {
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mAutoGpsLocationListener);

		} else if (isNetworkOnly && mNetworkOn) { // Else isNetworkOnly is true, then get Network reading
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mAutoGpsLocationListener);
		}

	}

	@Override
	protected String doInBackground(Object... args) {
		// Break this loop when reading is found or task cancellation happens
		while (!foundReadingFlag && !forceCancelTask) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}


		}

		if (gpsResult != null) {
			gpsResult = mLocation.getLatitude() + " " + mLocation.getLongitude() + " "
					+ mLocation.getAltitude() + " " + mLocation.getAccuracy();
		}

		return gpsResult;
	}

	@Override
	protected void onCancelled() {
		mLocationManager.removeUpdates(mAutoGpsLocationListener);
	}

	@Override
	protected void onPostExecute(String gpsResult) {
		mLocationManager.removeUpdates(mAutoGpsLocationListener);

		if (mGpsListener != null) {
			mGpsListener.autoGpsRecordingComplete(gpsResult);
		}
	}

	public void setAutoGpsRecordingListener(AutoGpsRecordingListener agrl) {
		mGpsListener = agrl;
	}

	public void forceCancelTask() {
		forceCancelTask = true;
	}

	public void stopLocationManager() {
		mLocationManager.removeUpdates(mAutoGpsLocationListener);
	}


	public class AutoGpsLocationListener implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {
			// Store the location, assign foundReadingFlag to true to break the main task loop
			mLocation = location;
			foundReadingFlag = true;
			Log.i(t, "Recording auto gps: " + System.currentTimeMillis() +
					" onStatusChanged() lat: " + mLocation.getLatitude() + " long: " +
					mLocation.getLongitude() + " alt: " + mLocation.getAltitude() +
					" acc: " + mLocation.getAccuracy());
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			switch (status) {
				case LocationProvider.AVAILABLE: // When available, gps should be recorded
					Log.i(t, "Recording auto gps: " + System.currentTimeMillis() +
							" onStatusChanged() lat: " + mLocation.getLatitude() + " long: " +
							mLocation.getLongitude() + " alt: " + mLocation.getAltitude() +
							" acc: " + mLocation.getAccuracy());
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
}