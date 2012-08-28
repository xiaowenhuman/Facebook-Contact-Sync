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

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;

/**
 * This class handles execution of batch mOperations on Contacts provider.
 */
final public class BatchOperation {
	private final String TAG = "BatchOperation";
	private final ContentResolver mResolver;
	// List for storing the batch mOperations
	private final ArrayList<ContentProviderOperation> mOperations;
	
	public BatchOperation(Context context, ContentResolver resolver) {
		mResolver = resolver;
		mOperations = new ArrayList<ContentProviderOperation>();
	}
	
	public int size() {
		return mOperations.size();
	}
	
	public void add(ContentProviderOperation cpo) {
		mOperations.add(cpo);
	}
	
	public Uri execute() {
		Uri result = null;
		
		if (mOperations.size() == 0) {
			return result;
		}
		// Apply the mOperations to the content provider
		try {
			ContentProviderResult[] results = mResolver.applyBatch(
					ContactsContract.AUTHORITY, mOperations);
			if ((results != null) && (results.length > 0))
				result = results[0].uri;
		} catch (final OperationApplicationException e1) {
			Log.e(TAG, "storing contact data failed", e1);
		} catch (final RemoteException e2) {
			Log.e(TAG, "storing contact data failed", e2);
		}
		mOperations.clear();
		return result;
	}
}
