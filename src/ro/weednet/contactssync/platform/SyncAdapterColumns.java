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
package ro.weednet.contactssync.platform;

import android.provider.ContactsContract.Data;

public final class SyncAdapterColumns {
	
	private SyncAdapterColumns() {
		
	}
	
	public static final String MIME_PROFILE = "vnd.android.cursor.item/vnd.facebook.profile";
	
	public static final String DATA_PID = Data.DATA1;
	
	public static final String DATA_SUMMARY = Data.DATA2;
	
	public static final String DATA_DETAIL = Data.DATA3;
}
