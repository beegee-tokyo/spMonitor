package tk.giesecke.spmonitor;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.os.StrictMode;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** spMonitor - Daily sync service
 *
 * Triggered daily to sync between the device and the spMonitor
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 *
 * @author Bernd Giesecke
 * @version 0.2 beta August 19, 2015.
 */
public class SyncService extends IntentService {

	public SyncService() {
		super("SyncService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (BuildConfig.DEBUG) Log.d("spMsync","started");

		if (intent != null) {

			boolean startFromUI = false;
			if (intent.hasExtra("START_FROM_UI")) {
				if (BuildConfig.DEBUG) Log.d("spMsync","Call from UI");
				startFromUI = true;
			} else {
				if (BuildConfig.DEBUG) Log.d("spMsync","Call from timer");
			}
			/** Context of application */
			Context intentContext = getApplicationContext();

			/** Access to shared preferences of app widget */
			SharedPreferences mPrefs = intentContext.getSharedPreferences("spMonitor", 0);

			// Try to sync only if we have connection and are on same WiFi as the spMonitor device
			if (Utilities.isConnectionAvailable(intentContext)) {
				/** SSID current connected or NULL if none */
				String connSSID = Utilities.getSSID(intentContext);
				if (connSSID != null && connSSID.equalsIgnoreCase(mPrefs.getString("SSID", "none"))) {
					/** URL of the spMonitor device */
					String deviceIP = mPrefs.getString("spMonitorIP", "no IP saved");

					// Make call only if valid url is given
					if (deviceIP.startsWith("No")) {
						if (BuildConfig.DEBUG) Log.d("spMsync","no valid IP");
					} else {
						if (android.os.Build.VERSION.SDK_INT > 9) {
							StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
							StrictMode.setThreadPolicy(policy);
						}

						// Get today's day for the online database name
						String[] dbNamesList = Utilities.getDateStrings();
						if (BuildConfig.DEBUG) Log.d("spMsync","This month = " + dbNamesList[0]);
						if (BuildConfig.DEBUG) Log.d("spMsync", "Last month = " + dbNamesList[1]);

						/** Instance of DataBaseHelper */
						DataBaseHelper dbHelper;
						/** Instance of data base */
						SQLiteDatabase dataBase;

						String syncedMonth = mPrefs.getString("synced_month", "");
						if (!syncedMonth.equalsIgnoreCase(dbNamesList[0])) {
							deleteDatabase(DataBaseHelper.DATABASE_NAME);
							deleteDatabase(DataBaseHelper.DATABASE_NAME_LAST);
							mPrefs.edit().putString("synced_month", dbNamesList[0]).apply();
							/** Instance of DataBaseHelper */
							dbHelper = new DataBaseHelper(this, DataBaseHelper.DATABASE_NAME);
							/** Instance of data base */
							dataBase = dbHelper.getReadableDatabase();
							dataBase.close();
							dbHelper.close();
							/** Instance of DataBaseHelper */
							dbHelper = new DataBaseHelper(this, DataBaseHelper.DATABASE_NAME_LAST);
							/** Instance of data base */
							dataBase = dbHelper.getReadableDatabase();
							dataBase.close();
							dbHelper.close();
						}

						// Sync this months database
						syncDB(DataBaseHelper.DATABASE_NAME, dbNamesList[0], deviceIP, intentContext);

						// Check if we have already synced the last month
						/** Instance of DataBaseHelper */
						dbHelper = new DataBaseHelper(intentContext, DataBaseHelper.DATABASE_NAME_LAST);
						/** Instance of data base */
						dataBase = dbHelper.getReadableDatabase();
						/** Cursor with data from database */
						Cursor dbCursor = DataBaseHelper.getLastRow(dataBase);
						if (dbCursor != null) {
							if (dbCursor.getCount() == 0) { // local database is empty, need to sync all data
								// Sync this months database
								syncDB(DataBaseHelper.DATABASE_NAME_LAST, dbNamesList[1], deviceIP, intentContext);
							}
						}
						if (dbCursor != null) {
							dbCursor.close();
						}
						dataBase.close();
						dbHelper.close();
					}
				}
			}
			if (startFromUI) {
				if (spMonitor.syncServiceReceiver != null) {
					Intent broadcastIntent = new Intent();
					broadcastIntent.setAction(spMonitor.DataServiceResponse.ACTION_RESP);
					broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
					try {
						sendBroadcast(broadcastIntent);
					} catch (RuntimeException e){
						if (BuildConfig.DEBUG) Log.d("spMsync","SendBroadCast to UI failed: "+e.toString());
					}
				}
			}
		}
	}

	/**
	 * Sync local database with the spMonitor database
	 *
	 * @param dbName
	 *        Name of database to be synced
	 * @param syncMonth
	 *        Month to be synced
	 * @param deviceIP
	 *        IP address of spMonitor
	 * @param intentContext
	 *        Context of this intent
	 */
	private void syncDB(String dbName, String syncMonth, String deviceIP, Context intentContext) {
		/** String with data received from spMonitor device */
		String resultData = "";
		/** A HTTP client to access the spMonitor device */
		OkHttpClient client = new OkHttpClient();

		/** String list with parts of the URL */
		String[] ipValues = deviceIP.split("/");
		/** URL to be called */
		String urlString = "http://"+ipValues[2]+"/sd/spMonitor/query2.php"; // URL to call

		// Check for last entry in the local database
		/** Instance of DataBaseHelper */
		DataBaseHelper dbHelper = new DataBaseHelper(intentContext, dbName);
		/** Instance of data base */
		SQLiteDatabase dataBase = dbHelper.getReadableDatabase();
		/** Cursor with data from database */
		Cursor dbCursor = DataBaseHelper.getLastRow(dataBase);
		/** Flag for database access type */
		boolean splitAccess = false;
		if (dbCursor.getCount() != 0) { // local database not empty, need to sync only missing
			dbCursor.moveToFirst();

			int lastMinute =  dbCursor.getInt(4);
			int lastHour = dbCursor.getInt(3);
			int lastDay = dbCursor.getInt(2);

			urlString += "?date=" + dbCursor.getString(0); // add year
			urlString += "-" + ("00" +
					dbCursor.getString(1)).substring(dbCursor.getString(1).length()); // add month
			urlString += "-" + ("00" +
					String.valueOf(lastDay))
					.substring(String.valueOf(lastDay).length()); // add day
			urlString += "-" + ("00" +
					String.valueOf(lastHour))
					.substring(String.valueOf(lastHour).length()); // add hour
			urlString += ":" + ("00" +
					String.valueOf(lastMinute))
					.substring(String.valueOf(lastMinute).length()); // add minute
			urlString += "&get=all";
		} else { // local database is empty, need to sync all data
			//urlString += "?date=" + syncMonth + "&get=all"; // get all of this month
			splitAccess = true;
			urlString += "?date=" + syncMonth;
		}
		dbCursor.close();
		dataBase.close();
		dbHelper.close();

		/** Repeat counter used when full database needs to be synced */
		int loopCnt = 0;
		/** URL used for access */
		String thisURL = urlString;
		if (splitAccess) {
			loopCnt = 3;
		}

		for (int loop = 0; loop <= loopCnt; loop++) {
			if (splitAccess) {
				urlString = thisURL + "-" + String.valueOf(loop);
				if (BuildConfig.DEBUG) Log.d("spMsync", "URL = " + urlString);
			}
			// Set timeout to 5 minutes in case we have a lot of data to load
			client.setConnectTimeout(5, TimeUnit.MINUTES); // connect timeout
			client.setReadTimeout(5, TimeUnit.MINUTES);    // socket timeout
			/** Request to spMonitor device */
			Request request = new Request.Builder()
					.url(urlString)
					.build();

			if (request != null) {
				try {
					/** Response from spMonitor device */
					Response response = client.newCall(request).execute();
					if (response != null) {
						resultData = response.body().string();
					}
				} catch (IOException ignore) {
				}
				try {
					/** JSON array with the data received from spMonitor device */
					JSONArray jsonFromDevice = new JSONArray(resultData);
					/** Instance of DataBaseHelper */
					dbHelper = new DataBaseHelper(intentContext, dbName);
					/** Instance of data base */

					dataBase = dbHelper.getWritableDatabase();
					if (dataBase == null) {
						dbHelper.close();
						return;
					}
					// Get received data into local database
					try {
						dataBase.beginTransactionNonExclusive();
					} catch (SQLiteDatabaseLockedException e) {
						dataBase.close();
						dbHelper.close();
						if (BuildConfig.DEBUG) Log.d("spMsync", "Database locked"+e);
						return;
					}
					for (int i=0; i<jsonFromDevice.length(); i++) {
						// skip first data record from device if we are just updating the database
						if (i == 0 && !splitAccess) i++;
						/** JSONObject with a single record */
						JSONObject jsonRecord = jsonFromDevice.getJSONObject(i);
						String record = jsonRecord.getString("d");
						record = record.replace("-",",");
						record += ","+jsonRecord.getString("l");
						record += ","+jsonRecord.getString("s");
						record += ","+jsonRecord.getString("c");
						DataBaseHelper.addDay(dataBase, record);
					}
					dataBase.setTransactionSuccessful();
					dataBase.endTransaction();
					if (BuildConfig.DEBUG) Log.d("spMsync","Sync successful for " + dbName);
				} catch (JSONException e) {
					if (BuildConfig.DEBUG) Log.d("spMsync","Sync JSON error for " + dbName + resultData);
					dataBase.endTransaction();
					dataBase.close();
					dbHelper.close();
				}
				dataBase.close();
				dbHelper.close();
			}
		}
	}
}
