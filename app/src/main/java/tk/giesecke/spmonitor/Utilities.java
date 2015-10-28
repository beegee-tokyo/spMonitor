package tk.giesecke.spmonitor;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/** spMonitor - Utilities
 *
 * Utilities used by SplashActivity and/or spMonitor
 *
 * @author Bernd Giesecke
 * @version 0.2 beta August 19, 2015.
 */
class Utilities {

	/**
	 * Scan the local subnet and return a list of IP addresses found
	 *
	 * @param subnet
	 *            Base IP address of the subnet
	 * @return <ArrayList>hosts</ArrayList>
	 *            Array list with all found IP addresses
	 */
	private static ArrayList<String> scanSubNet(String subnet){
		/** Array list to hold found IP addresses */
		ArrayList<String> hosts = new ArrayList<>();

		subnet = subnet.substring(0, subnet.lastIndexOf("."));
		subnet += ".";
		for(int i=0; i<255; i++){
			try {
				/** IP address under test */
				InetAddress inetAddress = InetAddress.getByName(subnet + String.valueOf(i));
				if(inetAddress.isReachable(500)){
					hosts.add(inetAddress.getHostName());
					Log.d("spMonitor", inetAddress.getHostName());
				}
			} catch (IOException e) {
				if (BuildConfig.DEBUG) Log.d("spMonitor", "Exception " + e);
			}
		}

		return hosts;
	}

	/**
	 * Sends a HTTP request to an IP to test if it is the spMonitor
	 *
	 * @param ip
	 *            IP address to test
	 * @return resultToDisplay
	 *            HTTP response from the IP
	 */
	public static String checkDeviceIP(String ip) {
		/** URL including command e to be send to the spMonitor device */
		String urlString="http://"+ip+"/arduino/e"; // URL to call
		/** Response from the URL under test */
		String resultToDisplay;

		spMonitor.client.setConnectTimeout(5, TimeUnit.SECONDS); // connect timeout
		spMonitor.client.setReadTimeout(5, TimeUnit.SECONDS);    // socket timeout

		/** Request to get HTTP content from URL */
		Request request = new Request.Builder()
				.url(urlString)
				.build();

		/** Response from spMonitor device */
		Response response;
		try {
			response = spMonitor.client.newCall(request).execute();
			resultToDisplay = response.body().string();
		} catch (IOException e) {
			if (BuildConfig.DEBUG) Log.d("spMonitor", "Empty reply");
			return "";
		}
		return resultToDisplay;
	}

	/**
	 * Customized alert
	 *
	 * @return <String>deviceIP</String>context
	 *            URL address of the spMonitor device
	 */
	public static String searchDeviceIP() {
		/** Instance of WiFi manager */
		WifiManager wm = (WifiManager) spMonitor.appContext.getSystemService(Context.WIFI_SERVICE);
		/** IP address assigned to this device */
		@SuppressWarnings("deprecation") String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
		/* List with all IP address found on this subnet */
		ArrayList<String> hosts = Utilities.scanSubNet(ip);
		spMonitor.deviceIP = "";
		for (int i=0; i<hosts.size(); i++) {
			ip = hosts.get(i);
			/** Result of check if spMonitor is on this IP address */
			String result = Utilities.checkDeviceIP(ip);
			if (result.startsWith("F ")) {
				spMonitor.deviceIP = "http://"+ip+"/arduino/";
				return spMonitor.deviceIP;
			}
		}
		return spMonitor.deviceIP;
	}

	/**
	 * Check if JSON object is valid
	 *
	 * @param test
	 *            String with JSON object or array
	 * @return boolean
	 *            true if "test" us a JSON object or array
	 *            false if no JSON object or array
	 */
	public static boolean isJSONValid(String test) {
		try {
			new JSONObject(test);
		} catch (JSONException ex) {
			try {
				new JSONArray(test);
			} catch (JSONException ex1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Start animation of refresh icon in action bar
	 */
	public static void startRefreshAnim() {
		/** Progressbar that will be shown during refresh */
		ProgressBar refreshRot = (ProgressBar) spMonitor.appView.findViewById(R.id.pb_refresh_rot);
		refreshRot.setVisibility(View.VISIBLE);
		spMonitor.isCommunicating = true;
	}

	/**
	 * Stop animation of refresh icon in action bar
	 */
	public static void stopRefreshAnim() {
		/** Progressbar that will be shown during refresh */
		ProgressBar refreshRot = (ProgressBar) spMonitor.appView.findViewById(R.id.pb_refresh_rot);
		refreshRot.setVisibility(View.INVISIBLE);
		spMonitor.isCommunicating = false;
	}

	/**
	 * Load plot series with data received from database
	 *
	 * @param data
	 *        database cursor with recorded values
	 *        each cursor entry has 8 values
	 *        cursor[0] = year stamp
	 *        cursor[1] = month stamp
	 *        cursor[2] = day stamp
	 *        cursor[3] = hour stamp
	 *        cursor[4] = minute stamp
	 *        cursor[5] = sensor power
	 *        cursor[6] = consumed power
	 *        cursor[7] = light value
	 */
	public static void fillSeries(Cursor data) {

		data.moveToFirst();
		spMonitor.solarEnergy = 0f;
		spMonitor.consEnergy = 0f;

		/** Array list to hold time stamps */
		ArrayList<String> tempTimeStamps;
		/** Array list to hold solar power values */
		ArrayList<Float> tempSolarStamps;
		/** Array list to hold consumption values */
		ArrayList<Float> tempConsPStamps;
		/** Array list to hold consumption values */
		ArrayList<Float> tempConsMStamps;
		/** Array list to hold light values */
		ArrayList<Long> tempLightStamps;
		if (spMonitor.showingLog) {
			tempTimeStamps = spMonitor.timeStamps;
			tempSolarStamps = spMonitor.solarPower;
			tempConsPStamps = spMonitor.consumPPower;
			tempConsMStamps = spMonitor.consumMPower;
			tempLightStamps = spMonitor.lightValue;
		} else {
			tempTimeStamps = spMonitor.timeStampsCont;
			tempSolarStamps = spMonitor.solarPowerCont;
			tempConsPStamps = spMonitor.consumPPowerCont;
			tempConsMStamps = spMonitor.consumMPowerCont;
			tempLightStamps = spMonitor.lightValueCont;
		}
		tempTimeStamps.clear();
		tempSolarStamps.clear();
		tempConsPStamps.clear();
		tempConsMStamps.clear();
		tempLightStamps.clear();
		for (int cursorIndex=0; cursorIndex<data.getCount(); cursorIndex++) {
			tempTimeStamps.add(("00" +
					data.getString(3)).substring(data.getString(3).length())
					+ ":" + ("00" +
					data.getString(4)).substring(data.getString(4).length()));
			spMonitor.dayToShow = String.valueOf(data.getInt(0)) + "-" +
					String.valueOf(data.getInt(1)) + "-" +
					String.valueOf(data.getInt(2));
			tempSolarStamps.add(data.getFloat(5));
			if (data.getFloat(6) < 0.0f) {
				tempConsPStamps.add(data.getFloat(6));
				tempConsMStamps.add(0.0f);
			} else {
				tempConsMStamps.add(data.getFloat(6));
				tempConsPStamps.add(0.0f);
			}
			// TODO for debugging only insert fake light value
			//tempLightStamps.add(data.getLong(7));
			tempLightStamps.add(data.getLong(5)*70);
			spMonitor.solarEnergy += data.getFloat(5)/60/1000;
			spMonitor.consEnergy += Math.abs(data.getFloat(6)/60/1000);
			data.moveToNext();
		}

		/** Text for update of text view */
		String updateTxt;
		if (spMonitor.showingLog) {
			/** Text view to show consumed / produced energy */
			TextView energyText = (TextView) spMonitor.appView.findViewById(R.id.tv_cons_energy);
			energyText.setVisibility(View.VISIBLE);
			updateTxt = "Consumed: " + String.format("%.3f", spMonitor.consEnergy) + "kWh";
			energyText.setText(updateTxt);
			energyText = (TextView) spMonitor.appView.findViewById(R.id.tv_solar_energy);
			energyText.setVisibility(View.VISIBLE);
			updateTxt = "Produced: " + String.format("%.3f", spMonitor.solarEnergy) + "kWh";
			energyText.setText(updateTxt);
		}
		/** Text view to show max consumed / produced power */
		if (tempConsMStamps.size() != 0 && tempSolarStamps.size() != 0) {
			TextView maxPowerText = (TextView) spMonitor.appView.findViewById(R.id.tv_cons_max);
			updateTxt = "(" + String.format("%.0f", Collections.max(tempConsMStamps)) + "W)";
			maxPowerText.setText(updateTxt);
			maxPowerText = (TextView) spMonitor.appView.findViewById(R.id.tv_solar_max);
			updateTxt = "(" + String.format("%.0f", Collections.max(tempSolarStamps)) + "W)";
			maxPowerText.setText(updateTxt);
		}
	}

	/**
	 * Get current date as integer
	 *
	 * @return <code>int[]</code>
	 *            Date as integer values
	 *            int[0] = year
	 *            int[1] = month
	 *            int[2] = day
	 */
	public static int[] getCurrentDate() {
		/** Integer array for return values */
		int[] currTime = new int[3];
		/** Calendar to get current time and date */
		Calendar cal = Calendar.getInstance();

		/** Today's month */
		currTime[1] = cal.get(Calendar.MONTH) + 1;

		/** Today's year */
		currTime[0] = cal.get(Calendar.YEAR);

		/** Today's day */
		currTime[2] = cal.get(Calendar.DATE);

		return currTime;
	}

	/**
	 * Get current month and last month as string
	 *
	 * @return <code>String[]</code>
	 *            [0] current month as string yy-mm
	 *            [1] last month as string yy-mm
	 */
	public static String[] getDateStrings() {
		/** Array with strings for this and last month date */
		String[] dateStrings = new String[2];
		/** Calendar to get current time and date */
		Calendar cal = Calendar.getInstance();
		/** Time format */
		@SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("yy-MM");
		dateStrings[0] = df.format(cal.getTime());
		cal.set(Calendar.MONTH, cal.get(Calendar.MONTH)-1);
		dateStrings[1] = df.format(cal.getTime());

		return dateStrings;
	}

	/**
	 * Get current time as string
	 *
	 * @return <code>String</code>
	 *            Time as string HH:mm
	 */
	public static String getCurrentTime() {
		/** Calendar to get current time and date */
		Calendar cal = Calendar.getInstance();
		/** Time format */
		@SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("HH:mm");
		return df.format(cal.getTime());
	}

	/**
	 * Add or subtract a day to/from current date
	 *
	 * @param fromDate
	 *              Date in format yy-MM-dd
	 * @param isAdd
	 *              Flag for adding / subtracting a day
	 *              true -> add a day
	 *              false -> subtract a day
	 * @return <code>String</code>
	 *              fromDate + 1 day
	 */
/*	public static String changeDay(String fromDate, boolean isAdd) {
		Calendar c = Calendar.getInstance();
		@SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("yy-MM-dd");
		try {
			Date myDate = df.parse(fromDate.trim());
			c.setTime(myDate);
			if (isAdd) {
				c.add(Calendar.DATE, 1);
			} else {
				c.add(Calendar.DATE, -1);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return df.format(c.getTime());
	}
*/
	/**
	 * Checks if external storage is available for read and write
	 *
	 * @return <code>boolean</code>
	 *            true if external storage is available
	 *            false if external storage is not available
	 */
	public static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}

	/**
	 * Pointer to directory or file on external storage
	 * If directory doesn't exist it will be created
	 *
	 * @param myFile
	 *            name of directory
	 * @param isDir
	 *            flag if it is a directory of file
	 *            true => directory (will try to create it if not existing)
	 *            false => file
	 * @return <code>File</code>
	 *            pointer to directory
	 */
	public static File getExFileDir(String myFile, boolean isDir) {
		// Get the directory for the user's public pictures directory.
		File file = new File(Environment.getExternalStorageDirectory(), myFile);
		if (isDir) {
			if (!file.mkdirs()) {
				if (BuildConfig.DEBUG) Log.d("spMonitor","Directory not created");
			}
		} else {
			//noinspection ResultOfMethodCallIgnored,ResultOfMethodCallIgnored
			file.delete();
		}
		return file;
	}

	/**
	 * Start or stop timer for widget updates
	 * If connection is same LAN as spMonitor device then update is every 60 seconds
	 * else the update is every 5 minutes
	 *
	 * @param context
	 *            application context
	 * @param isStart
	 *            flag if timer should be started or stopped
	 */
	public static void startStopWidgetUpdates(Context context, boolean isStart) {

		/** Intent to start scheduled update of the widgets */
		Intent timerIntent;
		/** Pending intent for broadcast message to update widgets */
		PendingIntent pendingIntent;
		/** Alarm manager for scheduled widget updates */
		AlarmManager alarmManager;
		/* Access to shared preferences of app widget */
		SharedPreferences mPrefs = context.getSharedPreferences("spMonitor", 0);
		/** Update interval in ms */
		int alarmTime = 60000;

		// Stop the update of the widgets
		/** Intent to stop scheduled update of the widgets */
		timerIntent = new Intent(SPwidget.SP_WIDGET_UPDATE);
		/** Pending intent for broadcast message to update widgets */
		pendingIntent = PendingIntent.getBroadcast(
				context, 2701, timerIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		/** Alarm manager for scheduled widget updates */
		alarmManager = (AlarmManager) context.getSystemService
				(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);

		if (isStart) {
			/** SSID of Wifi network */
			String connSSID = getSSID(context);

			if (!((connSSID != null) && (connSSID.equalsIgnoreCase(mPrefs.getString("SSID","none"))))) {
				/** Update interval in ms */
				alarmTime = 300000;
			}
			/** Intent for broadcast message to update widgets */
			timerIntent = new Intent(SPwidget.SP_WIDGET_UPDATE);
			/** Pending intent for broadcast message to update widgets */
			pendingIntent = PendingIntent.getBroadcast(
					context, 2701, timerIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			/** Alarm manager for scheduled widget updates */
			alarmManager = (AlarmManager) context.getSystemService
					(Context.ALARM_SERVICE);
			alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis(),
					alarmTime, pendingIntent);
		}
	}

	/**
	 * Start or stop timer for notification updates
	 * If connection is same LAN as spMonitor device then update is every 60 seconds
	 * else the update is every 5 minutes
	 *
	 * @param context
	 *            application context
	 * @param isStart
	 *            flag if timer should be started or stopped
	 */
	public static void startStopNotifUpdates(Context context, boolean isStart) {

		/** Intent to start scheduled update of the notifications */
		Intent notifIntent;
		/** Pending intent for broadcast message to update notifications */
		PendingIntent pendingIntent;
		/** Alarm manager for scheduled notifications updates */
		AlarmManager alarmManager;
		/* Access to shared preferences of app */
		SharedPreferences mPrefs = context.getSharedPreferences("spMonitor", 0);
		/** Update interval in ms */
		int alarmTime = 60000;

		// Stop the update of the notifications
		/** Intent to stop scheduled update of the notifications */
		notifIntent = new Intent(context, NotifService.class);
		/** Pending intent for broadcast message to update widgets */
		pendingIntent = PendingIntent.getBroadcast(
				context, 2703, notifIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		/** Alarm manager for scheduled widget updates */
		alarmManager = (AlarmManager) context.getSystemService
				(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);

		if (isStart) {
			/** SSID of Wifi network */
			String connSSID = getSSID(context);

			if (!((connSSID != null) && (connSSID.equalsIgnoreCase(mPrefs.getString("SSID","none"))))) {
				/** Update interval in ms */
				alarmTime = 300000;
			}
			/** Pending intent for notification updates */
			PendingIntent pi = PendingIntent.getService(context, 2703,
					new Intent(context, NotifService.class),PendingIntent.FLAG_UPDATE_CURRENT);
			/** Alarm manager for daily sync */
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			am.setRepeating(AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis() + 10000,
					alarmTime, pi);
		}
	}

	/**
	 * Check WiFi connection and return SSID
	 *
	 * @param context
	 *            application context
	 * @return <code>String</code>
	 *            SSID name or NULL if not connected
	 */
	public static String getSSID(Context context) {
		/** Access to connectivity manager */
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		/** WiFi connection information  */
		android.net.NetworkInfo wifiOn = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (!wifiOn.isConnected()) {
			return null;
		} else {
			final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
			if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
				return connectionInfo.getSSID();
			}
		}
		return null;
	}

	/**
	 * Get list of all available notification tones
	 *
	 * @param context
	 *            application context
	 * @param notifNames
	 *            array list to store the name of the tones
	 * @param notifUri
	 *            array list to store the paths of the tones
	 */
	public static int getNotifSounds(Context context, ArrayList<String> notifNames, ArrayList<String> notifUri) {
		/** Instance of the ringtone manager */
		RingtoneManager manager = new RingtoneManager(context);
		manager.setType(RingtoneManager.TYPE_NOTIFICATION);
		/** Cursor with the notification tones */
		Cursor cursor = manager.getCursor();
		/** Access to shared preferences of application*/
		SharedPreferences mPrefs = context.getSharedPreferences("spMonitor", 0);
		/** Last user selected alarm tone */
		String lastUri = mPrefs.getString("alarmUri","");
		/** Index of lastUri in the list */
		int uriIndex = -1;

		while (cursor.moveToNext()) {
			notifNames.add(cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX));
			notifUri.add(cursor.getString(RingtoneManager.URI_COLUMN_INDEX) + "/" +
					cursor.getString(RingtoneManager.ID_COLUMN_INDEX));
			if (lastUri.equalsIgnoreCase(cursor.getString(RingtoneManager.URI_COLUMN_INDEX) + "/" +
					cursor.getString(RingtoneManager.ID_COLUMN_INDEX))) {
				uriIndex = cursor.getPosition();
			}
		}
		return uriIndex;
	}

	/**
	 * Check if an internet connection is available
	 *
	 * @param context
	 *            Context of application
	 *
	 * @return <code>boolean</code>
	 *            True if we have connection
	 *            False if we do not have connection
	 */
	public static boolean isConnectionAvailable(Context context) {
		/** Flag for WiFi available */
		boolean bHaveWiFi;
		/** Flag for mobile connection available */
		boolean bHaveMobile;

		/** Access to connectivity manager */
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		android.net.NetworkInfo wifiOn = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		android.net.NetworkInfo mobileOn = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		// Check if wifi is available && if we are connected
		// Result is false if there is no Wifi on the device
		// Result is true if we have Wifi
		// else result is false
		if (wifiOn != null) {
			if (wifiOn.isConnected()) {
				if (BuildConfig.DEBUG) Log.d("spMonitor", "We have WiFi ");
				bHaveWiFi = true;
			} else {
				bHaveWiFi = false;
			}
		} else {
			bHaveWiFi = false;
		}

		// Check if we have mobile && if mobile is allowed && if roaming is on && allowed
		// Result is false if there is no mobile on the device
		// Result is true if we have mobile
		if (mobileOn != null) {
			if (mobileOn.isConnected()) {
				if (BuildConfig.DEBUG) Log.d("spMonitor", "We have Mobile ");
				bHaveMobile = true;
			} else {
				bHaveMobile = false;
			}
		} else {
			bHaveMobile = false;
		}

		return bHaveWiFi || bHaveMobile;
	}

	/**
	 * Start day dream if it is enabled by the user
	 *
	 * @param context
	 *            Context of application
	 */
	public static void startDayDreaming(Context context) {
		// Check if Android version supports day dream
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			// Check if day dream is enabled
			if (Settings.Secure.getInt(context.getContentResolver(), SCREENSAVER_ENABLED , -1) == 1) {
				String availDayDreams = Settings.Secure.getString(context.getContentResolver(), SCREENSAVER_COMPONENTS );
				String[] namesArray = availDayDreams.split(",");
				ComponentName[] componentNames = new ComponentName[namesArray.length];
				for (int i = 0; i < namesArray.length; i++) {
					componentNames[i] = ComponentName.unflattenFromString(namesArray[i]);
				}
				String selectedDayDream = componentNames[0].getClassName();
				if (BuildConfig.DEBUG) Log.d("spMonitor", "Avail day dreams: "+availDayDreams);
				if (BuildConfig.DEBUG) Log.d("spMonitor", "Selected day dream: "+selectedDayDream);

				if (selectedDayDream.equalsIgnoreCase("tk.giesecke.spmonitor.SolarDayDream")) {
					final Intent intent = new Intent(Intent.ACTION_MAIN);
					intent.setClassName("com.android.systemui", "com.android.systemui.Somnambulator");
					context.startActivity(intent);
					return;
				}
			}
		}
		// TODO start our own (fake) daydream activity -  for now we open daydream settings
		startDaydreamsSettings(context);
	}

	/**
	 * Starts the Daydream settings menu page of the Android settings menu
	 * @param context
	 *          the context to use
	 * @throws ActivityNotFoundException when there's no Daydream settings page found
	 */
	private static void startDaydreamsSettings(Context context) throws ActivityNotFoundException {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			context.startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
		} else {
			// Running JB_MR1, fall back to constant string
			context.startActivity(new Intent(ACTION_DREAM_SETTINGS));
		}
	}

	/** *******************************************
	 * Gotta love that Android is open source :)
	 * ********************************************/

	/**
	 * Whether screensavers are enabled. (integer value, 1 if enabled)
	 */
	private static final String SCREENSAVER_ENABLED = "screensaver_enabled";

	/**
	 * The user's chosen screensaver components. (string value)
	 *
	 * These will be launched by the PhoneWindowManager after a timeout when not on
	 * battery, or upon dock insertion (if SCREENSAVER_ACTIVATE_ON_DOCK is set to 1).
	 */
	private static final String SCREENSAVER_COMPONENTS = "screensaver_components";

	/**
	 * Activity Action: Show Daydream settings.
	 * <p>
	 * In some cases, a matching Activity may not exist, so ensure you
	 * safeguard against this.
	 */
	private static final String ACTION_DREAM_SETTINGS = "android.settings.DREAM_SETTINGS";

	/**
	 * Return notification icon ID as int
	 * @param currPower
	 *          power value as float
	 * @return <code>int</code>
	 *          ID of matching icon
	 */
	public static int getNotifIcon(float currPower) {
		if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			if (currPower > 0.0d) {
				return R.drawable.arrow_red_down_small;
			} else {
				return R.drawable.arrow_green_up_small;
			}
		}

		if (currPower < -400) {
			return R.drawable.m400;
		} else if (currPower < -350) {
			return R.drawable.m350;
		} else if (currPower < -300) {
			return R.drawable.m300;
		} else if (currPower < -250) {
			return R.drawable.m250;
		} else if (currPower < -200) {
			return R.drawable.m200;
		} else if (currPower < -150) {
			return R.drawable.m150;
		} else if (currPower < -100) {
			return R.drawable.m100;
		} else if (currPower < -50) {
			return R.drawable.m50;
		} else if (currPower < 0){
			return R.drawable.m0;
		} else if (currPower < 50) {
			return R.drawable.p0;
		} else if (currPower < 100) {
			return R.drawable.p50;
		} else if (currPower < 150) {
			return R.drawable.p100;
		} else if (currPower < 200) {
			return R.drawable.p150;
		} else if (currPower < 250) {
			return R.drawable.p200;
		} else if (currPower < 300) {
			return R.drawable.p250;
		} else if (currPower < 350) {
			return R.drawable.p300;
		} else if (currPower < 400) {
			return R.drawable.p350;
		} else {
			return R.drawable.p400;
		}
	}
}
