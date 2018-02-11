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
import org.odk.collect.android.preferences.GeneralSharedPreferences;
import org.odk.collect.android.preferences.PreferenceKeys;

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
	private Location mLocationBackup;
	private LocationManager mLocationManager;
	private boolean mGPSOn = false;
	private boolean mNetworkOn = false;

	private boolean isNetworkOnly;
	private boolean foundReadingFlag = false;
	private boolean forceCancelTask = false;
	private Integer GPS_TIMEOUT;

	private AutoGpsRecordingListener mGpsListener;
	private AutoGpsLocationListener mAutoGpsLocationListener;
	private String gpsResult;

	public RecordAutoGpsTask(LocationManager locationManager) {
		mLocationManager = locationManager;
	}

	@Override
	protected void onPreExecute() {
		mAutoGpsLocationListener = new AutoGpsLocationListener();
		mLocation = mLocationBackup = null;
		gpsResult = null;

		String autoGpsProvider = (String) GeneralSharedPreferences.getInstance().get(PreferenceKeys.KEY_AUTO_GPS_PROVIDER);

		if (autoGpsProvider.equals(PreferenceKeys.AUTO_GPS_PROVIDER_NETWORK)) {
			isNetworkOnly = true;
		} else {
			isNetworkOnly = false;
		}

		String gpsTimeout = (String) GeneralSharedPreferences.getInstance().get(PreferenceKeys.KEY_GPS_TIMEOUT);
		GPS_TIMEOUT = Integer.parseInt(gpsTimeout) * 1000;

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

		} else if (!isNetworkOnly && mGPSOn) {
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mAutoGpsLocationListener);

			// Also start the Network gps, which will be used as a backup in case we don't get a reading
			// from satellite Gps
			if (mNetworkOn) {
				mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mAutoGpsLocationListener);
			}

		} else if (isNetworkOnly && mNetworkOn) { // Else isNetworkOnly is true, then get Network reading
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mAutoGpsLocationListener);
		}

	}

	@Override
	protected String doInBackground(Object... args) {
		long start = System.currentTimeMillis();

		// Break this loop when reading is found or task cancellation happens
		while (!foundReadingFlag && !forceCancelTask) {
			try {
				Thread.sleep(200);
				long elapsedTime = System.currentTimeMillis() - start;

				// If timeout occurred while getting Gps then break the loop and finish the task
				if (elapsedTime >= GPS_TIMEOUT) {
					break;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (mLocation == null && mLocationBackup != null) {
			mLocation = mLocationBackup;
		}

		if (mLocation != null) {
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
			mGpsListener.autoGpsRecordingComplete(gpsResult, mLocation.getProvider());
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
			// If provider is Gps, and network returns a reading, then don't break the loop
			// But still record the Network gps as backup
			if (!isNetworkOnly && location.getProvider().equalsIgnoreCase(LocationManager.NETWORK_PROVIDER)) {
				mLocationBackup = location;
			} else {
				// Store the location, assign foundReadingFlag to true to break the main task loop
				mLocation = location;
				foundReadingFlag = true;

				Log.i(t, "Recording auto gps: " + System.currentTimeMillis() +
						" onStatusChanged() lat: " + mLocation.getLatitude() + " long: " +
						mLocation.getLongitude() + " alt: " + mLocation.getAltitude() +
						" acc: " + mLocation.getAccuracy());
			}

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