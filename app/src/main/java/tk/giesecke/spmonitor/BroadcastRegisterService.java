package tk.giesecke.spmonitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

/**
 * BroadcastRegisterService
 * Registers broadcast receiver for screen on/off broadcast messages
 *
 * @author Bernd Giesecke
 * @version 0.2 beta August 19, 2015.
 */
public class BroadcastRegisterService extends Service {

	/** Receiver for screen on/off broadcast msgs */
	public static BroadcastReceiver mReceiver = null;

	public BroadcastRegisterService() {
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

		/** Context of application */
		Context intentContext = getApplicationContext();
		/** Access to shared preferences of app widget */
		SharedPreferences mPrefs = intentContext.getSharedPreferences("spMonitor", 0);
		/** Update interval in ms */
		int alarmTime = 60000;

		if ((mPrefs.getBoolean("notif",true)) || (mPrefs.getInt("wNums",0) != 0)) {
			/** IntentFilter to receive screen on/off broadcast msgs */
			IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			filter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
			/** Receiver for screen on/off broadcast msgs */
			mReceiver = new EventReceiver();
			registerReceiver(mReceiver, filter);

			/** Pending intent for updates */
			PendingIntent pi = PendingIntent.getService(intentContext, 3001,
					new Intent(intentContext, UpdateService.class),PendingIntent.FLAG_UPDATE_CURRENT);
			/** Alarm manager for daily sync */
			AlarmManager am = (AlarmManager) intentContext.getSystemService(Context.ALARM_SERVICE);
			am.setRepeating(AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis() + 10000,
					alarmTime, pi);
		}


		/** Pending intent for daily sync */
		PendingIntent pi = PendingIntent.getService(intentContext, 3002,
				new Intent(intentContext, SyncService.class),PendingIntent.FLAG_UPDATE_CURRENT);
		/** Alarm manager for sync every 2 hours*/
		AlarmManager am = (AlarmManager) intentContext.getSystemService(Context.ALARM_SERVICE);
		// TODO testing hourly update of the database
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10000,
				7200000, pi);
	}
}
