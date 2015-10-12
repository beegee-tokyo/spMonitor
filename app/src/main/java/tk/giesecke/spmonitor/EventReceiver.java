package tk.giesecke.spmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * EventReceiver
 * Get screen on/off broadcast messages to start/stop the update of the app widgets
 *
 * @author Bernd Giesecke
 * @version 0.2 beta August 19, 2015.
 */
public class EventReceiver extends BroadcastReceiver {

	public EventReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		/* Access to shared preferences of app widget */
		SharedPreferences mPrefs = context.getSharedPreferences("spMonitor", 0);

		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			if (BuildConfig.DEBUG) Log.d("spMonitor Event", "Screen off");
			if (mPrefs.getInt("wNums", 0) != 0) {
				Utilities.startStopWidgetUpdates(context, false);
			} else if (mPrefs.getBoolean("notif",true)) {
				Utilities.startStopNotifUpdates(context, false);
			}
		} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			if (BuildConfig.DEBUG) Log.d("spMonitor Event", "Screen on");
			if (mPrefs.getInt("wNums", 0) != 0) {
				Utilities.startStopWidgetUpdates(context, true);
			} else if (mPrefs.getBoolean("notif",true)) {
				Utilities.startStopNotifUpdates(context, true);
			}
		} if (intent.getAction().equals
				(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
			// CONNECTIVITY CHANGE
			if (BuildConfig.DEBUG) Log.d("spMonitor Event", "Connection Change");
			if (Utilities.isConnectionAvailable(context)) {
				if (mPrefs.getInt("wNums", 0) != 0) {
					Utilities.startStopWidgetUpdates(context, true);
				} else if (mPrefs.getBoolean("notif",true)) {
					Utilities.startStopNotifUpdates(context, true);
				}
			} else {
				if (mPrefs.getInt("wNums", 0) != 0) {
					Utilities.startStopWidgetUpdates(context, false);
				} else if (mPrefs.getBoolean("notif",true)) {
					Utilities.startStopNotifUpdates(context, false);
				}
			}
		}
	}
}
