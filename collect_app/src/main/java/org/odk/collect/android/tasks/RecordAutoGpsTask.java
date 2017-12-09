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

/**
 * The background task to automatically collect gps
 * This is done when there is a gps question with binding "auto_gps"
 *
 * @author Raghu Mittal
 */
public class RecordAutoGpsTask extends AsyncTask<Object, Void, String> implements LocationListener {
	private static final String t = "RecordAutoGpsTask";
	private Location mLocation;
	private LocationManager mLocationManager;

	//private RecordGpsListener mGpsListener;
	//private String gps;

	@Override
	protected String doInBackground(Object... args) {
		//android.os.Debug.waitForDebugger();
		Looper.prepare();

		mLocationManager = (LocationManager) args[0];
		mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
		Looper.loop(); // start waiting...when this is done, we'll have the location in this.mLocation


        /*String url = values[0];
		Collect.getInstance().getActivityLogger().logAction(this, "/authUser", url);

        // get shared HttpContext so that authentication and cookies are retained.
        HttpContext localContext = Collect.getInstance().getHttpContext();
        HttpClient httpclient = WebUtils.createHttpClient(WebUtils.CONNECTION_TIMEOUT);

        DocumentFetchResult result = WebUtils.getAuthResult(url, localContext, httpclient);

        if (result.errorMessage != null) {
            return result.errorMessage;
        }*/

		return "Success";
	}

	@Override
	protected void onPostExecute(String value) {
		/*synchronized (this) {
            if (mStateListener != null) {
                mStateListener.userAuthenticationComplete(value);
            }
        }*/
	}

    /*public void setAuthenticationListener(UserAuthenticationListener sl) {
        synchronized (this) {
            mStateListener = sl;
        }
    }*/

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