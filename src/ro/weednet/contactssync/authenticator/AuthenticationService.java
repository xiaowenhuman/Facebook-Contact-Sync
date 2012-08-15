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
