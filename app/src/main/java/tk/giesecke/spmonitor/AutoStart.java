package tk.giesecke.spmonitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/** spMonitor - AutoStart
 *
 * Start refresh timer for app widgets (if widgets are placed
 *
 * @author Bernd Giesecke
 * @version 0.2 beta August 19, 2015.
 */
public class AutoStart extends BroadcastReceiver {

	public AutoStart() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
//			/** Access to shared preferences of app widget */
//			SharedPreferences mPrefs = context.getSharedPreferences("spMonitor", 0);
//			/** Update interval in ms */
//			int alarmTime = 60000;
//			if (mPrefs.getInt("wNums",0) != 0) {
//
//				/** Intent for broadcast message to update widgets */
//				Intent widgetIntent = new Intent(SPwidget.SP_WIDGET_UPDATE);
//				/** Pending intent for broadcast message to update widgets */
//				PendingIntent pendingWidgetIntent = PendingIntent.getBroadcast(
//						context, 2701, widgetIntent, PendingIntent.FLAG_CANCEL_CURRENT);
//				/** Alarm manager for scheduled widget updates */
//				AlarmManager alarmManager = (AlarmManager) context.getSystemService
//						(Context.ALARM_SERVICE);
//				alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
//						System.currentTimeMillis() + 10000,
//						alarmTime, pendingWidgetIntent);
//
//			} else {
//				if (mPrefs.getBoolean("notif",true)) {
//					/** Pending intent for notification updates */
//					PendingIntent pi = PendingIntent.getService(context, 2703,
//							new Intent(context, NotifService.class),PendingIntent.FLAG_UPDATE_CURRENT);
//					/** Alarm manager for daily sync */
//					AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//					am.setRepeating(AlarmManager.RTC_WAKEUP,
//							System.currentTimeMillis() + 10000,
//							alarmTime, pi);
//				}
//			}

			// Start service to register BroadcastRegisterService
			context.startService(new Intent(context, BroadcastRegisterService.class));

			/** Calendar instance to setup daily sync */
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, 7); // trigger at 7am
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			/** Pending intent for daily sync */
			PendingIntent pi = PendingIntent.getService(context, 2702,
					new Intent(context, SyncService.class),PendingIntent.FLAG_UPDATE_CURRENT);
			/** Alarm manager for daily sync */
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
					AlarmManager.INTERVAL_DAY, pi);
		}
	}
}
