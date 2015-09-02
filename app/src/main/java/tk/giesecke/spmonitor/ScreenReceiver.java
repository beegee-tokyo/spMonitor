package tk.giesecke.spmonitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;

/**
 * ScreenReceiver
 * Get screen on/off broadcast messages to start/stop the update of the app widgets
 *
 * @author Bernd Giesecke
 * @version 0.2 beta August 19, 2015.
 */
public class ScreenReceiver extends BroadcastReceiver {

	public ScreenReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		/* Access to shared preferences of app widget */
		SharedPreferences mPrefs = context.getSharedPreferences("spMonitor",0);
		/** Calendar to get current time and date */
		Calendar cal = Calendar.getInstance();

		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			if (mPrefs.getInt("wNums", 0) != 0) {
				if (cal.get(Calendar.HOUR_OF_DAY)<7 || cal.get(Calendar.HOUR_OF_DAY)>17) {
					if (BuildConfig.DEBUG) Log.d("spMonitor ScreenService", "Screen off");
					/** Intent to start scheduled update of the widgets */
					Intent stopIntent = new Intent(SPwidget.SP_WIDGET_UPDATE);
					/** Pending intent for broadcast message to update widgets */
					PendingIntent pendingIntent = PendingIntent.getBroadcast(
							context, 2701, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
					/** Alarm manager for scheduled widget updates */
					AlarmManager alarmManager = (AlarmManager) context.getSystemService
							(Context.ALARM_SERVICE);
					alarmManager.cancel(pendingIntent);
				}
			}
		} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			if (mPrefs.getInt("wNums", 0) != 0) {
				/** Today's month */
				if (cal.get(Calendar.HOUR_OF_DAY)>7 || cal.get(Calendar.HOUR_OF_DAY)<17) {
					if (BuildConfig.DEBUG) Log.d("spMonitor ScreenService", "Screen on");
					/** Update interval in ms */
					int alarmTime = 60000;

					/** Intent for broadcast message to update widgets */
					Intent startIntent = new Intent(SPwidget.SP_WIDGET_UPDATE);
					/** Pending intent for broadcast message to update widgets */
					PendingIntent pendingIntent = PendingIntent.getBroadcast(
							context, 2701, startIntent, PendingIntent.FLAG_CANCEL_CURRENT);
					/** Alarm manager for scheduled widget updates */
					AlarmManager alarmManager = (AlarmManager) context.getSystemService
							(Context.ALARM_SERVICE);
					alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
							System.currentTimeMillis(),
							alarmTime, pendingIntent);
				}
			}
		}
	}
}
