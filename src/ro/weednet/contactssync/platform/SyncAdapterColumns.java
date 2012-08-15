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
