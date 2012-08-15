package ro.weednet.contactssync.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ro.weednet.ContactsSync;
import ro.weednet.contactssync.Constants;
import ro.weednet.contactssync.R;
import ro.weednet.contactssync.authenticator.AuthenticatorActivity;
import ro.weednet.contactssync.client.RawContact;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;

public class Preferences extends PreferenceActivity {
	public final static int DEFAULT_SYNC_FREQUENCY = 24;//hours
	public final static int DEFAULT_PICTURE_SIZE = RawContact.IMAGE_SIZES.SQUARE;
	public final static boolean DEFAULT_SYNC_ALL = true;
	public final static boolean DEFAULT_SYNC_WIFI_ONLY = false;
	public final static boolean DEFAULT_JOIN_BY_ID = false;
	public final static boolean DEFAULT_SYNC_BIRTHDAYS = false;
	public final static boolean DEFAULT_SYNC_STATUSES = true;
	public final static boolean DEFAULT_SHOW_NOTIFICATIONS = false;
	public final static int DEFAULT_CONNECTION_TIMEOUT = 60;
	private Dialog mAuthDialog;
	private Account[] mAccounts;
	
	@SuppressWarnings("serial")
	private List<Map<String, String>> _questions = new ArrayList<Map<String, String>>() {
		private static final long serialVersionUID = 7572191145554574231L;
		{
			add(new HashMap<String, String> () { { put("name", "Some of my friends are missing. Why?"); }; });
			add(new HashMap<String, String> () { { put("name", "Is my connection and account secure?"); }; });
			add(new HashMap<String, String> () { { put("name", "How do I merge duplicated contacts?"); }; });
			add(new HashMap<String, String> () { { put("name", "I am getting a lot of connection errors. What should I do?"); }; });
			add(new HashMap<String, String> () { { put("name", "I am getting a lot of sign-in errors. What should I do?"); }; });
			add(new HashMap<String, String> () { { put("name", "The picture quality is crappy. Why?"); }; });
			add(new HashMap<String, String> () { { put("name", "Birthdays are incorrect or missing."); }; });
		}
	};
	@SuppressWarnings("serial")
	private List<List<Map<String, String>>> _answers = new ArrayList<List<Map<String, String>>>() {
		private static final long serialVersionUID = 5046933056181371918L;
		{
			add(new ArrayList<Map<String, String>>() {
				{
					add(new HashMap<String, String> () { { put("name", "Facebook users have the option to block one or all apps."); }; });
					add(new HashMap<String, String> () { { put("name", "If they opt for that, they will be excluded from your friends list."); }; });
					add(new HashMap<String, String> () { { put("name", "The only thing you can do is to ask them to change that setting."); }; });
				}
			});
			add(new ArrayList<Map<String, String>>() {
				{
					add(new HashMap<String, String> () { { put("name", "Yes."); }; });
					add(new HashMap<String, String> () { { put("name", "This application uses a secure connection (HTTPS) and stores nothing outside of your phone."); }; });
					add(new HashMap<String, String> () { { put("name", "Unless your phone gets hacked, your account is safe and your privacy is protected."); }; });
				}
			});
			add(new ArrayList<Map<String, String>>() {
				{
					add(new HashMap<String, String> () { { put("name", "You have two options:"); }; });
					add(new HashMap<String, String> () { { put("name", "1. Manually join them. (Edit contact -> Join)"); }; });
					add(new HashMap<String, String> () { { put("name", "2. Add their Facebook ID in the \"Notes\" field in your local/Google contact. They will be automatically joined. (You might have to run a full sync)"); }; });
				}
			});
			add(new ArrayList<Map<String, String>>() {
				{
					add(new HashMap<String, String> () { { put("name", "If you get this errors when your phone is in standby (closed screen), it's normal. Just disable the notifications from settings. Some phones have a policy to disable WiFi and/or 3G when they are in standby, in order to save battery."); }; });
					add(new HashMap<String, String> () { { put("name", "If you get it when the screen is on, it's probably an connection problem. Try again later."); }; });
				}
			});
			add(new ArrayList<Map<String, String>>() {
				{
					add(new HashMap<String, String> () { { put("name", "You should get this error only if you changed your Facebook password or if you manually removed the app permissions from Facebook."); }; });
					add(new HashMap<String, String> () { { put("name", "If this is not the case and you still get this error repeatedly, try removing the account and add it again."); }; });
				}
			});
			add(new ArrayList<Map<String, String>>() {
				{
					add(new HashMap<String, String> () { { put("name", "All the Android devices have a maximum size for contact pictures."); }; });
					add(new HashMap<String, String> () { { put("name", "If you select a bigger image size, it will be automatically scaled down."); }; });
					add(new HashMap<String, String> () { { put("name", "For example: Nexus One has 96x96, Galaxy Nexus with ICS has 256x256. In Jelly Bean this setting has been increased to 720x720."); }; });
				}
			});
			add(new ArrayList<Map<String, String>>() {
				{
					add(new HashMap<String, String> () { { put("name", "I am sorry but birthdays only work on some devices."); }; });
					add(new HashMap<String, String> () { { put("name", "In future updates this might be fixed, but until then, if it doesn't work properly just disable the option."); }; });
				}
			});
		}
	};
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		addPreferencesFromResource(R.xml.preferences_sync);
		addPreferencesFromResource(R.xml.preferences_troubleshooting);
		addPreferencesFromResource(R.xml.preferences_about);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
		
		ContactsSync app = ContactsSync.getInstance();
		
		AccountManager am = AccountManager.get(Preferences.this);
		mAccounts = am.getAccountsByType(Constants.ACCOUNT_TYPE);
		
		if (mAccounts.length > 0) {
			if (ContentResolver.getSyncAutomatically(mAccounts[0], ContactsContract.AUTHORITY)) {
				if (app.getSyncFrequency() == 0) {
					app.setSyncFrequency(DEFAULT_SYNC_FREQUENCY);
					app.savePreferences();
					ContentResolver.addPeriodicSync(mAccounts[0], ContactsContract.AUTHORITY, new Bundle(), DEFAULT_SYNC_FREQUENCY * 3600);
				}
			} else {
				if (app.getSyncFrequency() > 0) {
					app.setSyncFrequency(0);
					app.savePreferences();
				}
			}
			
			findPreference("sync_freq").setOnPreferenceChangeListener(syncFreqChange);
			findPreference("pic_size").setOnPreferenceChangeListener(picSizeChange);
			findPreference("sync_all").setOnPreferenceChangeListener(syncAllChange);
			findPreference("sync_wifi_only").setOnPreferenceChangeListener(syncWifiOnlyChange);
			findPreference("sync_join_by_id").setOnPreferenceChangeListener(syncJoinByIdChange);
			findPreference("sync_birthdays").setOnPreferenceChangeListener(syncBirthdaysChange);
			findPreference("sync_statuses").setOnPreferenceChangeListener(syncStatusesChange);
			findPreference("show_notif").setOnPreferenceChangeListener(showNotificationsChange);
			findPreference("conn_timeout").setOnPreferenceChangeListener(connectionTimeoutChange);
			
			findPreference("faq").setOnPreferenceClickListener(faqClick);
			findPreference("run_now").setOnPreferenceClickListener(syncNowClick);
			findPreference("run_now_full").setOnPreferenceClickListener(syncFullNowClick);
			
			Intent test_intent = new Intent(this, TestFacebookApi.class);
			findPreference("test_fb_api").setIntent(test_intent);
			
			Intent email_intent = new Intent(Intent.ACTION_SENDTO);
			email_intent.setData(Uri.parse("mailto:" + getString(R.string.contact_email)
					+ "?subject=Bug%20report%20for%20application%20" + getString(R.string.app_name)));
			findPreference("contact_bug").setIntent(email_intent);
			
			String donateUrl = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=7SUETTCRKTMKY";
			Intent donate_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(donateUrl));
			findPreference("donate").setIntent(donate_intent);
			
			String version = "";
			try {
				version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			} catch (NameNotFoundException e1) {
				
			}
			findPreference("version").setSummary(version);
			
			boolean market_installed = true;
			try {
				getPackageManager().getPackageInfo("com.android.vending", 0);
			} catch (PackageManager.NameNotFoundException e) {
				market_installed = false;
			}
			if (market_installed) {
				Intent rate_intent = new Intent(Intent.ACTION_VIEW);
				rate_intent.setData(Uri.parse("market://details?id=ro.weednet.contactssync"));
				findPreference("rate_app").setIntent(rate_intent);
			} else {
				findPreference("rate_app").setEnabled(false);
			}
		} else {
			mAuthDialog = new Dialog(this);
			mAuthDialog.setContentView(R.layout.not_account_actions);
			mAuthDialog.setTitle("Select option");
			((Button) mAuthDialog.findViewById(R.id.add_account_button)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mAuthDialog.dismiss();
					Intent intent = new Intent(Preferences.this, AuthenticatorActivity.class);
					startActivity(intent);
				}
			});
			((Button) mAuthDialog.findViewById(R.id.exit_button)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mAuthDialog.dismiss();
					Preferences.this.finish();
				}
			});
			mAuthDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					mAuthDialog.dismiss();
					Preferences.this.finish();
				}
			});
			mAuthDialog.show();
		}
	}
	
	Preference.OnPreferenceChangeListener syncFreqChange = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			try {
				ContactsSync app = ContactsSync.getInstance();
				app.setSyncFrequency(Integer.parseInt((String) newValue));
				
				int sync_freq = app.getSyncFrequency() * 3600;
				
				AccountManager am = AccountManager.get(Preferences.this);
				Account[] accounts = am.getAccountsByType(Constants.ACCOUNT_TYPE);
				
				if (sync_freq > 0) {
					ContentResolver.setSyncAutomatically(accounts[0], ContactsContract.AUTHORITY, true);
					
					Bundle extras = new Bundle();
					ContentResolver.addPeriodicSync(accounts[0], ContactsContract.AUTHORITY, extras, sync_freq);
				} else {
					ContentResolver.setSyncAutomatically(accounts[0], ContactsContract.AUTHORITY, false);
				}
				
				return true;
			} catch (Exception e) {
				Log.d("contactsync-preferences", "error: " + e.getMessage());
				return false;
			}
		}
	};
	Preference.OnPreferenceChangeListener picSizeChange = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			try {
				ContactsSync app = ContactsSync.getInstance();
				app.setPictureSize(Integer.parseInt((String) newValue));
				
				return true;
			} catch (Exception e) {
				Log.d("contactsync-preferences", "error: " + e.getMessage());
				return false;
			}
		}
	};
	Preference.OnPreferenceChangeListener syncAllChange = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			try {
				final ContactsSync app = ContactsSync.getInstance();
				
				if ((Boolean) newValue == true) {
					app.setSyncAllContacts(true);
					return true;
				} else {
					new AlertDialog.Builder(Preferences.this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle("Confirm")
					.setMessage("This action will trigger a full sync. It will use more bandwidth and remove all manual contact joins. Are you sure you want to do this?")
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							app.setSyncAllContacts(false);
							app.requestFullSync();
							((CheckBoxPreference) findPreference("sync_all")).setChecked(false);
						}
					})
					.setNegativeButton("No", null)
					.show();
					return false;
				}
			} catch (Exception e) {
				Log.d("contactsync-preferences", "error: " + e.getMessage());
				return false;
			}
		}
	};
	Preference.OnPreferenceChangeListener syncWifiOnlyChange = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			try {
				ContactsSync app = ContactsSync.getInstance();
				app.setSyncWifiOnly((Boolean) newValue);
				return true;
			} catch (Exception e) {
				Log.d("contactsync-preferences", "error: " + e.getMessage());
				return false;
			}
		}
	};
	Preference.OnPreferenceChangeListener syncJoinByIdChange = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			try {
				ContactsSync app = ContactsSync.getInstance();
				app.setJoinById((Boolean) newValue);
				return true;
			} catch (Exception e) {
				Log.d("contactsync-preferences", "error: " + e.getMessage());
				return false;
			}
		}
	};
	Preference.OnPreferenceChangeListener syncBirthdaysChange = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			try {
				ContactsSync app = ContactsSync.getInstance();
				app.setSyncBirthdays((Boolean) newValue);
				return true;
			} catch (Exception e) {
				Log.d("contactsync-preferences", "error: " + e.getMessage());
				return false;
			}
		}
	};
	Preference.OnPreferenceChangeListener syncStatusesChange = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			try {
				ContactsSync app = ContactsSync.getInstance();
				app.setSyncStatuses((Boolean) newValue);
				return true;
			} catch (Exception e) {
				Log.d("contactsync-preferences", "error: " + e.getMessage());
				return false;
			}
		}
	};
	Preference.OnPreferenceChangeListener showNotificationsChange = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			try {
				ContactsSync app = ContactsSync.getInstance();
				app.setShowNotifications((Boolean) newValue);
				return true;
			} catch (Exception e) {
				Log.d("contactsync-preferences", "error: " + e.getMessage());
				return false;
			}
		}
	};
	Preference.OnPreferenceChangeListener connectionTimeoutChange = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			try {
				ContactsSync app = ContactsSync.getInstance();
				app.setConnectionTimeout(Integer.parseInt((String) newValue));
				return true;
			} catch (Exception e) {
				Log.d("contactsync-preferences", "error: " + e.getMessage());
				return false;
			}
		}
	};
	Preference.OnPreferenceClickListener syncNowClick = new Preference.OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (mAccounts.length == 0) {
				return false;
			}
			
			ContactsSync app = ContactsSync.getInstance();
			
			if (app.getSyncWifiOnly() && !app.wifiConnected()) {
				Toast toast = Toast.makeText(Preferences.this, "Blocked. Wifi only .. ", Toast.LENGTH_LONG);
				toast.show();
				
				return false;
			}
			
			ContentResolver.requestSync(mAccounts[0], ContactsContract.AUTHORITY, new Bundle());
			
			Toast toast = Toast.makeText(Preferences.this, "Sync started .. ", Toast.LENGTH_LONG);
			toast.show();
			
			return true;
		}
	};
	Preference.OnPreferenceClickListener syncFullNowClick = new Preference.OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (mAccounts.length == 0) {
				return false;
			}
			
			final ContactsSync app = ContactsSync.getInstance();
			
			if (app.getSyncWifiOnly() && !app.wifiConnected()) {
				Toast toast = Toast.makeText(Preferences.this, "Blocked. Wifi only .. ", Toast.LENGTH_LONG);
				toast.show();
				
				return false;
			}
			
			new AlertDialog.Builder(Preferences.this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle("Confirm")
				.setMessage("Are you sure you want run a full sync? It will use more bandwidth and remove all manual contact joins.")
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						app.requestFullSync();
						ContentResolver.requestSync(mAccounts[0], ContactsContract.AUTHORITY, new Bundle());
						
						Toast toast = Toast.makeText(Preferences.this, "Full Sync started .. ", Toast.LENGTH_LONG);
						toast.show();
					}
				})
				.setNegativeButton("No", null)
				.show();
			
			return true;
		}
	};
	Preference.OnPreferenceClickListener faqClick = new Preference.OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			Dialog faqDialog = new Dialog(Preferences.this);
			faqDialog.setContentView(R.layout.faq);
			faqDialog.setTitle("FAQ:");
			
			// Set up our adapter
			final ExpandableListAdapter mAdapter = new SimpleExpandableListAdapter(
				Preferences.this,
				_questions,
				R.layout.faq_row,
				new String[] { "name" },
				new int[] { R.id.question },
				_answers,
				R.layout.answer,
				new String[] { "name" },
				new int[] { R.id.category_list_name}
			);
			
			ExpandableListView categ_list = (ExpandableListView) faqDialog.findViewById(R.id.exp_list);
			categ_list.setAdapter(mAdapter);
			
			faqDialog.show();
			
			return false;
		}
	};
}
