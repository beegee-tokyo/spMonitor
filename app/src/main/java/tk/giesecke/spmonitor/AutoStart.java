package tk.giesecke.spmonitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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
			// Start service to register BroadcastRegisterService
			context.startService(new Intent(context, BroadcastRegisterService.class));

			/** Pending intent for daily sync */
			PendingIntent pi = PendingIntent.getService(context, 2702,
					new Intent(context, SyncService.class),PendingIntent.FLAG_UPDATE_CURRENT);
			/** Alarm manager for sync every 2 hours*/
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			// TODO testing hourly update of the database
			am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10000,
					7200000, pi);
		}
	}
}
