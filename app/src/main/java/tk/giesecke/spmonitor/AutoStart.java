package tk.giesecke.spmonitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

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
			/** Access to shared preferences of app widget */
			SharedPreferences wPrefs = context.getSharedPreferences("WidgetValues",0);
			if (wPrefs.getInt("wNums",0) != 0) {
				/** Update interval in ms */
				int alarmTime = 60000;

				/** Intent for broadcast message to update widgets */
				Intent widgetIntent = new Intent(SPwidget.SP_WIDGET_UPDATE);
				/** Pending intent for broadcast message to update widgets */
				PendingIntent pendingWidgetIntent = PendingIntent.getBroadcast(
						context, 2701, widgetIntent, PendingIntent.FLAG_CANCEL_CURRENT);
				/** Alarm manager for scheduled widget updates */
				AlarmManager alarmManager = (AlarmManager) context.getSystemService
						(Context.ALARM_SERVICE);
				alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
						System.currentTimeMillis() + 10000,
						alarmTime, pendingWidgetIntent);

				/** IntentFilter to receive Screen on/off broadcast msgs */
				IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
				filter.addAction(Intent.ACTION_SCREEN_OFF);
				/** BroadcastReceiver to receive Screen on/off broadcast msgs */
				ScreenReceiver.screenOnOffReceiver = new ScreenReceiver();
				context.registerReceiver(ScreenReceiver.screenOnOffReceiver, filter);
			}
		}
	}
}
