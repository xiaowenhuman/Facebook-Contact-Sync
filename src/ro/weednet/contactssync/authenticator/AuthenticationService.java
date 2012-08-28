/*
 * Copyright (C) 2012 Danut Chereches
 *
 * Contact: Danut Chereches <admin@weednet.ro>
 *
 * This file is part of Facebook Contact Sync.
 * 
 * Facebook Contact Sync is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Facebook Contact Sync.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 */
package ro.weednet.contactssync.authenticator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AuthenticationService extends Service {
	private static final String TAG = "AuthenticationService";
	private Authenticator mAuthenticator;
	
	@Override
	public void onCreate() {
		Log.v(TAG, "SyncAdapter Authentication Service started.");
		mAuthenticator = new Authenticator(this);
	}
	
	@Override
	public void onDestroy() {
		Log.v(TAG, "SyncAdapter Authentication Service stopped.");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "getBinder()...  returning the AccountAuthenticator binder for intent " + intent);
		return mAuthenticator.getIBinder();
	}
}
