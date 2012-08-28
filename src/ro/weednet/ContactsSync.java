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
package ro.weednet;

import ro.weednet.contactssync.activities.Preferences;
import ro.weednet.contactssync.client.RawContact;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ContactsSync extends Application {
	private final static String _namespace = "ro.weednet.contactssync_preferences";
	private int mSyncFreq;
	private int mPicSize;
	private boolean mSyncAll;
	private boolean mFullSync;
	private boolean mSyncWifiOnly;
	private boolean mJoinById;
	private boolean mSyncBirthdays;
	private boolean mSyncStatuses;
	private boolean mShowNotifications;
	private int mConnTimeout;
	private static ContactsSync _instance = null;
	
	public static ContactsSync getInstance() {
		if(_instance != null) {
			return _instance;
		} else {
			return new ContactsSync();
		}
	}
	
	private SharedPreferences getSharedPreferences() {
		return getSharedPreferences(_namespace, MODE_PRIVATE);
	}
	
	public Context getContext() {
		return super.getApplicationContext();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		_instance = this;
		
		reloadPreferences();
	}
	
	public int getSyncFrequency() {
		return mSyncFreq;
	}
	public int getPictureSize() {
		return mPicSize;
	}
	public boolean getSyncAllContacts() {
		return mSyncAll;
	}
	public boolean getSyncWifiOnly() {
		return mSyncWifiOnly;
	}
	public boolean getJoinById() {
		return mJoinById;
	}
	public boolean getSyncBirthdays() {
		return mSyncBirthdays;
	}
	public boolean getSyncStatuses() {
		return mSyncStatuses;
	}
	public boolean getFullSync() {
		return mFullSync;
	}
	public boolean getShowNotifications() {
		return mShowNotifications;
	}
	public int getConnectionTimeout() {
		return mConnTimeout;
	}
	
	public void setSyncFrequency(int value) {
		if (value >= 0 || value <= 720) {
			mSyncFreq = value;
		}
	}
	public void setPictureSize(int value) {
		if (value == RawContact.IMAGE_SIZES.SMALL_SQUARE
		 || value == RawContact.IMAGE_SIZES.SMALL
		 || value == RawContact.IMAGE_SIZES.NORMAL
		 || value == RawContact.IMAGE_SIZES.BIG
		 || value == RawContact.IMAGE_SIZES.SQUARE
		 || value == RawContact.IMAGE_SIZES.BIG_SQUARE
		 || value == RawContact.IMAGE_SIZES.HUGE_SQUARE) {
			mPicSize = value;
		}
	}
	public void setSyncAllContacts(boolean value) {
		mSyncAll = value;
	}
	public void setSyncWifiOnly(boolean value) {
		mSyncWifiOnly = value;
	}
	public void setJoinById(boolean value) {
		mJoinById = value;
	}
	public void setSyncBirthdays(boolean value) {
		mSyncBirthdays = value;
	}
	public void setSyncStatuses(boolean value) {
		mSyncStatuses = value;
	}
	public void setShowNotifications(boolean value) {
		mShowNotifications = value;
	}
	public void setConnectionTimeout(int value) {
		mConnTimeout = Math.min(Math.max(value, 0), 3600);
	}
	public void requestFullSync() {
		mFullSync = true;
		SharedPreferences.Editor editor = getSharedPreferences().edit();
		editor.putBoolean("full_sync", mFullSync);
		editor.commit();
	}
	public void clearFullSync() {
		mFullSync = false;
		SharedPreferences.Editor editor = getSharedPreferences().edit();
		editor.putBoolean("full_sync", mFullSync);
		editor.commit();
	}
	
	public void reloadPreferences() {
		SharedPreferences settings = getSharedPreferences();
		
		try {
			mSyncFreq = Integer.parseInt(settings.getString("sync_freq", Integer.toString(Preferences.DEFAULT_SYNC_FREQUENCY)));//hours
		} catch (NumberFormatException e) {
			mSyncFreq = Preferences.DEFAULT_SYNC_FREQUENCY;
		}
		try {
			mPicSize = Integer.parseInt(settings.getString("pic_size", Integer.toString(Preferences.DEFAULT_PICTURE_SIZE)));
		} catch (NumberFormatException e) {
			mPicSize = Preferences.DEFAULT_PICTURE_SIZE;
		}
		mSyncAll = settings.getBoolean("sync_all", Preferences.DEFAULT_SYNC_ALL);
		mSyncWifiOnly = settings.getBoolean("sync_wifi_only", Preferences.DEFAULT_SYNC_WIFI_ONLY);
		mJoinById = settings.getBoolean("sync_join_by_id", Preferences.DEFAULT_JOIN_BY_ID);
		mSyncBirthdays = settings.getBoolean("sync_birthdays", Preferences.DEFAULT_SYNC_BIRTHDAYS);
		mSyncStatuses = settings.getBoolean("sync_statuses", Preferences.DEFAULT_SYNC_STATUSES);
		mFullSync = settings.getBoolean("full_sync", false);
		mShowNotifications = settings.getBoolean("show_notif", Preferences.DEFAULT_SHOW_NOTIFICATIONS);
		try {
			mConnTimeout = Integer.parseInt(settings.getString("conn_timeout", Integer.toString(Preferences.DEFAULT_CONNECTION_TIMEOUT)));
		} catch (NumberFormatException e) {
			mConnTimeout = Preferences.DEFAULT_CONNECTION_TIMEOUT;
		}
	}
	
	public void savePreferences() {
		SharedPreferences.Editor editor = getSharedPreferences().edit();
		editor.putString("sync_freq", Integer.toString(mSyncFreq));
		editor.putString("pic_size", Integer.toString(mPicSize));
		editor.putBoolean("sync_all", mSyncAll);
		editor.putBoolean("sync_wifi_only", mSyncWifiOnly);
		editor.putBoolean("sync_join_by_id", mJoinById);
		editor.putBoolean("sync_birthdays", mSyncBirthdays);
		editor.putBoolean("sync_statuses", mSyncStatuses);
		editor.putBoolean("full_sync", mFullSync);
		editor.putString("conn_timeout", Integer.toString(mConnTimeout));
		editor.commit();
	}
	
	public boolean wifiConnected() {
		ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = null;
		
		if (connectivityManager != null) {
			networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		}
		
		return networkInfo == null ? false : networkInfo.isConnected();
	}
}
