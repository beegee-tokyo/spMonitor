package tk.giesecke.spmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			if (BuildConfig.DEBUG) Log.d("spMonitor Event", "Screen off");
			Utilities.startStopUpdates(context, false);
		} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			if (BuildConfig.DEBUG) Log.d("spMonitor Event", "Screen on");
				Utilities.startStopUpdates(context, true);
		} if (intent.getAction().equals
				(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
			// CONNECTIVITY CHANGE
			if (BuildConfig.DEBUG) Log.d("spMonitor Event", "Connection Change");
			if (Utilities.isConnectionAvailable(context)) {
					Utilities.startStopUpdates(context, true);
			} else {
					Utilities.startStopUpdates(context, false);
			}
		}
	}
}
