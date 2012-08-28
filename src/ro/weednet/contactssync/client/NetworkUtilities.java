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
package ro.weednet.contactssync.client;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ro.weednet.ContactsSync;
import ro.weednet.contactssync.authenticator.Authenticator;

import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;

import android.accounts.Account;
import android.accounts.NetworkErrorException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Provides utility methods for communicating with the server.
 */
final public class NetworkUtilities {
	private Facebook mFacebook = new Facebook("104789639646317");
	
	public NetworkUtilities(String token) {
		mFacebook.setAccessToken(token);
	}
	
	/**
	 * Connects to the Sync test server, authenticates the provided
	 * username and password.
	 * 
	 * @param username
	 *            The server account username
	 * @param password
	 *            The server account password
	 * @return String The authentication token returned by the server (or null)
	 * @throws NetworkErrorException 
	 */
	public boolean checkAccessToken() throws NetworkErrorException {
	//	throw new NetworkErrorException("custom");
		
		try {
			Bundle params = new Bundle();
			params.putInt("timeout", ContactsSync.getInstance().getConnectionTimeout() * 1000);
			
			mFacebook.extendAccessTokenIfNeeded(ContactsSync.getInstance(), null);
			
			try {
				String response = mFacebook.request("me/permissions", params);
				JSONObject json = Util.parseJson(response);
				JSONObject permissions = json.getJSONArray("data").getJSONObject(0);
				for (int i = 0; i < Authenticator.REQUIRED_PERMISSIONS.length; i++) {
					if (permissions.isNull(Authenticator.REQUIRED_PERMISSIONS[i])
					 || permissions.getInt(Authenticator.REQUIRED_PERMISSIONS[i]) == 0) {
						Log.v("checkToken", "failed because of permissions");
						return false;
					}
				}
				return true;
			} catch (FacebookError e) {
				Log.v("checkToken", "facebook error, code: " + e.getErrorCode() + ", message: " + e.getMessage());
				if (!e.getErrorType().equals("OAuthException")) {
					throw new NetworkErrorException(e.getMessage());
				}
			} catch (JSONException e) {
				Log.v("checkToken", "json error: " + e.getMessage());
				throw new NetworkErrorException(e.getMessage());
			}
		} catch (IOException e) {
			Log.v("checkToken", "ioexception: " + e.getMessage());
			throw new NetworkErrorException(e.getMessage());
		}
		
		Log.v("checkToken", "failed. returning false.");
		
		return false;
	}
	
	public List<RawContact> getContacts(Account account)
			throws JSONException, ParseException, IOException, AuthenticationException {
		
		final ArrayList<RawContact> serverList = new ArrayList<RawContact>();
		ContactsSync app = ContactsSync.getInstance();
		int pictureSize = app.getPictureSize();
		String pic_size = null;
		boolean album_picture = false;
		
		switch (pictureSize) {
			case RawContact.IMAGE_SIZES.SMALL_SQUARE:
				pic_size = "pic_square";
				break;
			case RawContact.IMAGE_SIZES.SMALL:
				pic_size = "pic_small";
				break;
			case RawContact.IMAGE_SIZES.NORMAL:
				pic_size = "pic";
				break;
			case RawContact.IMAGE_SIZES.SQUARE:
			case RawContact.IMAGE_SIZES.BIG_SQUARE:
			case RawContact.IMAGE_SIZES.HUGE_SQUARE:
				album_picture = true;
			case RawContact.IMAGE_SIZES.BIG:
				pic_size = "pic_big";
				break;
		}
		
		String fields = "uid, first_name, last_name, " + pic_size;
		
		if (app.getSyncStatuses()) {
			fields += ", status";
		}
		if (app.getSyncBirthdays()) {
			fields += ", birthday, birthday_date";
		}
		
		boolean more = true;
		int limit;
		int offset = 0;
		while (more) {
			more = false;
			Bundle params = new Bundle();
			
			if (album_picture) {
				limit = 300;
				String query1 = "SELECT " + fields + " FROM user WHERE uid IN (SELECT uid2 FROM friend WHERE uid1 = me()) LIMIT " + limit + " OFFSET " + offset;
				String query2 = "SELECT owner, src_big, modified FROM photo WHERE pid IN (SELECT cover_pid FROM album WHERE owner IN (SELECT uid FROM #query1) AND type = 'profile')";
				params.putString("method", "fql.multiquery");
				params.putString("queries", "{\"query1\":\"" + query1 + "\", \"query2\":\"" + query2 + "\"}");
			} else {
				limit = 1000;
				String query = "SELECT " + fields + " FROM user WHERE uid IN (SELECT uid2 FROM friend WHERE uid1 = me()) LIMIT " + limit + " OFFSET " + offset;
				params.putString("method", "fql.query");
				params.putString("query", query);
			}
			params.putInt("timeout", app.getConnectionTimeout() * 1000);
			String response = mFacebook.request(params);
			
			if (response != null) {
				try {
					JSONArray serverContacts;
					HashMap<String, JSONObject> serverImages = new HashMap<String, JSONObject>();
					if (album_picture) {
						JSONArray result = new JSONArray(response);
						serverContacts = result.getJSONObject(0).getJSONArray("fql_result_set");
						JSONArray images = result.getJSONObject(1).getJSONArray("fql_result_set");
						JSONObject image;
						for (int j = 0; j < images.length(); j++) {
							image = images.getJSONObject(j);
							serverImages.put(image.getString("owner"), image);
						}
					} else {
						serverContacts = new JSONArray(response);
					}
					
					JSONObject contact;
					for (int i = 0; i < serverContacts.length(); i++) {
						contact = serverContacts.getJSONObject(i);
						if (album_picture && serverImages.containsKey(contact.getString("uid"))) {
							contact.put("picture", serverImages.get(contact.getString("uid")).getString("src_big"));
						} else {
							contact.put("picture", !contact.isNull(pic_size) ? contact.getString(pic_size) : null);
						}
						RawContact rawContact = RawContact.valueOf(contact);
						if (rawContact != null) {
							serverList.add(rawContact);
						}
					}
					
					if (serverContacts.length() > limit / 2) {
						offset += limit;
						more = true;
					}
				} catch (Exception e) {
					try {
						JSONObject r = new JSONObject(response);
						if (!r.isNull("error_code") && r.getInt("error_code") == 190) {
							throw new AuthenticationException();
						}
						else
						{
							throw new ParseException(r.getString("error_msg"));
						}
					} catch (JSONException e2) { }
					
					Log.e("network_utils", "api error");
					throw new ParseException();
				}
			} else {
				Log.e("network_utils", "Server error");
				throw new IOException();
			}
		}
		
		return serverList;
	}
	
	/**
	 * Download the avatar image from the server.
	 * 
	 * @param avatarUrl
	 *            the URL pointing to the avatar image
	 * @return a byte array with the raw JPEG avatar image
	 */
	public static byte[] downloadAvatar(final String avatarUrl) {
		// If there is no avatar, we're done
		if (TextUtils.isEmpty(avatarUrl)) {
			return null;
		}
		
		try {
			URL url = new URL(avatarUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			try {
				BitmapFactory.Options options = new BitmapFactory.Options();
				Bitmap originalImage = BitmapFactory.decodeStream(connection.getInputStream(), null, options);
				ByteArrayOutputStream convertStream;
				
				if (ContactsSync.getInstance().getPictureSize() == RawContact.IMAGE_SIZES.SQUARE
				 || ContactsSync.getInstance().getPictureSize() == RawContact.IMAGE_SIZES.BIG_SQUARE
				 || ContactsSync.getInstance().getPictureSize() == RawContact.IMAGE_SIZES.HUGE_SQUARE) {
					int targetWidth, targetHeight;
					switch(ContactsSync.getInstance().getPictureSize()) {
						case RawContact.IMAGE_SIZES.HUGE_SQUARE:
							targetWidth  = 720;
							targetHeight = 720;
							break;
						case RawContact.IMAGE_SIZES.BIG_SQUARE:
							targetWidth  = 512;
							targetHeight = 512;
							break;
						case RawContact.IMAGE_SIZES.SQUARE:
						default:
							targetWidth  = 256;
							targetHeight = 256;
					}
					Log.v("pic_size", "w:"+targetWidth + ", h:"+targetHeight);
					
					int cropWidth = Math.min(originalImage.getWidth(), originalImage.getHeight());
					int cropHeight = cropWidth;
					int offsetX = Math.round((originalImage.getWidth() - cropWidth) / 2);
					int offsetY = Math.round((originalImage.getHeight() - cropHeight) / 2);
					
					Bitmap croppedImage = Bitmap.createBitmap(originalImage, offsetX, offsetY, cropWidth, cropHeight);
					Bitmap resizedBitmap = Bitmap.createScaledBitmap(croppedImage, targetWidth, targetHeight, false);
					
					convertStream = new ByteArrayOutputStream(targetWidth * targetHeight * 4);
					resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, convertStream);
					
					croppedImage.recycle();
					resizedBitmap.recycle();
				} else {
					convertStream = new ByteArrayOutputStream(originalImage.getWidth() * originalImage.getHeight() * 4);
					originalImage.compress(Bitmap.CompressFormat.JPEG, 95, convertStream);
				}
				
				convertStream.flush();
				convertStream.close();
				originalImage.recycle();
				return convertStream.toByteArray();
			} finally {
				connection.disconnect();
			}
		} catch (MalformedURLException muex) {
			// A bad URL - nothing we can really do about it here...
			Log.e("network_utils", "Malformed avatar URL: " + avatarUrl);
		} catch (IOException ioex) {
			// If we're unable to download the avatar, it's a bummer but not the
			// end of the world. We'll try to get it next time we sync.
			Log.e("network_utils", "Failed to download user avatar: " + avatarUrl);
		} catch (NullPointerException npe) {
			// probably `avatar` is null
			Log.e("network_utils", "Failed to download user avatar: " + avatarUrl);
		}
		return null;
	}
}
