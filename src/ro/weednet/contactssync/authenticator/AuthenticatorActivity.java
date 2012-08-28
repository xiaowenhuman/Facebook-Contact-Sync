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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

import ro.weednet.ContactsSync;
import ro.weednet.contactssync.Constants;
import ro.weednet.contactssync.R;
import ro.weednet.contactssync.activities.Preferences;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Util;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {
	Facebook mFacebook;
	private AccountManager mAccountManager;
	public static final String PARAM_USERNAME = "fb_email";
	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";
	protected boolean mRequestNewAccount = false;
	private String mFbEmail;
	private AsyncFacebookRunner mAsyncRunner;
	public final Handler mHandler = new Handler();
	protected ProgressDialog mLoading;
	protected AlertDialog mDialog;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		mLoading = new ProgressDialog(this);
		mLoading.setTitle(getText(R.string.app_name));
		mLoading.setMessage("Loading ... ");
	//	mLoading.setCancelable(false);
		mLoading.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				mHandler.post(new Runnable() {
					public void run() {
						AuthenticatorActivity.this.finish();
					}
				});
			}
		});
		mFacebook = new Facebook(getString(R.string.facebook_app_id));
		mAccountManager = AccountManager.get(this);
		mAsyncRunner = new AsyncFacebookRunner(mFacebook);
		
		final Intent intent = getIntent();
		mFbEmail = intent.getStringExtra(PARAM_USERNAME);
		mRequestNewAccount = mFbEmail == null;
		
		mFacebook.authorize(this, Authenticator.REQUIRED_PERMISSIONS, new DialogListener() {
			@Override
			public void onComplete(Bundle values) {
				mHandler.post(new Runnable() {
					public void run() {
						mLoading.show();
					}
				});
				
				mAsyncRunner.request("me", new getUserInfo());
			}
			
			@Override
			public void onFacebookError(FacebookError error) {
				Log.v("facebook", "onFacebookError");
				mHandler.post(new DisplayException(error.getMessage()));
			}
			
			@Override
			public void onError(DialogError e) {
				Log.v("facebook", "onError");
				mHandler.post(new DisplayException(e.getMessage()));
			}
			
			@Override
			public void onCancel() {
				Log.v("facebook", "onCancel");
				mHandler.post(new Runnable() {
					public void run() {
						AuthenticatorActivity.this.finish();
					}
				});
			}
		});
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (mLoading != null) {
			try {
				mLoading.dismiss();
			} catch (Exception e) {
				
			}
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		mFacebook.authorizeCallback(requestCode, resultCode, data);
	}
	
	public class getUserInfo implements RequestListener {
		@Override
		public void onMalformedURLException(MalformedURLException e, Object state) {
			mHandler.post(new DisplayException(e.getMessage()));
		}
		
		@Override
		public void onIOException(IOException e, Object state) {
			mHandler.post(new DisplayException(e.getMessage()));
		}
		
		@Override
		public void onFileNotFoundException(FileNotFoundException e, Object state) {
			mHandler.post(new DisplayException(e.getMessage()));
		}
		
		@Override
		public void onFacebookError(FacebookError e, Object state) {
			mHandler.post(new DisplayException(e.getMessage()));
		}
		
		@Override
		public void onComplete(String response, Object state) {
			try {
				JSONObject json = Util.parseJson(response);
				
				ContactsSync app = ContactsSync.getInstance();
				app.setConnectionTimeout(Preferences.DEFAULT_CONNECTION_TIMEOUT);
				app.savePreferences();
				final String email = json.getString("email");
				final String access_token = mFacebook.getAccessToken();
				final int sync_freq = app.getSyncFrequency() * 3600;
				
				final Account account = new Account(email, Constants.ACCOUNT_TYPE);
				if (mRequestNewAccount) {
					mAccountManager.addAccountExplicitly(account, access_token, null);
				} else {
					mAccountManager.setPassword(account, access_token);
				}
				
				if (sync_freq > 0) {
					ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
					
					Bundle extras = new Bundle();
					ContentResolver.addPeriodicSync(account, ContactsContract.AUTHORITY, extras, sync_freq);
				} else {
					ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, false);
				}
				
				mHandler.post(new Runnable() {
					public void run() {
						if (mLoading != null) {
							try {
								mLoading.dismiss();
							} catch (Exception e) {
								
							}
						}
						final Intent intent = new Intent();
						intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, email);
						intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
						intent.putExtra(AccountManager.KEY_AUTHTOKEN, access_token);
						setAccountAuthenticatorResult(intent.getExtras());
						setResult(RESULT_OK, intent);
						finish();
					}
				});
			} catch (JSONException e) {
				Log.w("Facebook", "JSON Error in response");
			} catch (FacebookError e) {
				Log.w("Facebook", "Facebook Error: " + e.getMessage());
			}
		}
	}
	
	protected class DisplayException implements Runnable {
		String mMessage;
		
		public DisplayException(String msg) {
			mMessage = msg;
		}
		
		public void run() {
			AlertDialog.Builder builder = new AlertDialog.Builder(AuthenticatorActivity.this);
			builder.setTitle("Facebook Error");
			builder.setMessage(mMessage);
			builder.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					mDialog.dismiss();
					mLoading.dismiss();
					AuthenticatorActivity.this.finish();
				}
			});
			try {
				mDialog = builder.create();
				mDialog.show();
			} catch (Exception e) {
				
			}
		}
	}
}
