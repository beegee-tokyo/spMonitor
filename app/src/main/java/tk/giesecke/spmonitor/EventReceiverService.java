package tk.giesecke.spmonitor;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

/**
 * EventReceiverService
 * Registers broadcast receiver for screen on/off broadcast messages
 *
 * @author Bernd Giesecke
 * @version 0.2 beta August 19, 2015.
 */
public class EventReceiverService extends Service {
	public EventReceiverService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		/** Access to shared preferences of app widget */
		SharedPreferences wPrefs = this.getSharedPreferences("spMonitor", 0);
		if (BuildConfig.DEBUG) Log.d("spMonitor ScreenService", "Widget number = " + wPrefs.getInt("wNums", 0));
		if (wPrefs.getInt("wNums", 0) != 0) {
			/** IntentFilter to receive screen on/off broadcast msgs */
			IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			filter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
			/** Receiver for screen on/off broadcast msgs */
			BroadcastReceiver mReceiver = new EventReceiver();
			registerReceiver(mReceiver, filter);
		}
	}
}
