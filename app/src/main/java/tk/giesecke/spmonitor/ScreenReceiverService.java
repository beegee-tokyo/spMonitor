package tk.giesecke.spmonitor;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

/**
 * ScreenReceiverService
 * Registers broadcast receiver for screen on/off broadcast messages
 *
 * @author Bernd Giesecke
 * @version 0.2 beta August 19, 2015.
 */
public class ScreenReceiverService extends Service {
	public ScreenReceiverService() {
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
			/** IntentFilter to receive Screen on/off broadcast msgs */
			IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			/** BroadcastReceiver to receive Screen on/off broadcast msgs */
			ScreenReceiver.screenOnOffReceiver = new ScreenReceiver();
			this.registerReceiver(ScreenReceiver.screenOnOffReceiver, filter);
		}
	}
}
